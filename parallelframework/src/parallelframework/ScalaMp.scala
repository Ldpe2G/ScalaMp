package parallelframework

import akka.actor._
import ActorAdmin._
import Worker._
import scala.collection.Map
import akka.pattern.{ask}
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import scala.util._
import scala.concurrent.Await
import akka.dispatch.Futures

object ScalaMp{
  
  val actorSystem = ActorSystem("ScalaMp")
  val actorAdmin = actorSystem.actorOf(Props[ActorAdmin]) 
  actorAdmin ! InitThreads
  
  var allRange: Option[Range] = None 
  var threadNum: Option[Int] = None
  
  implicit val timeout = Timeout(100 days)
  
	
	def critical(region: => Unit) = {
		actorAdmin ! CriticalRegion(() => region)
		/*val resul = actorAdmin ? CriticalRegion(() => region)
		Await.result(resul, Duration.Inf) match {
  			case _ => 
  		}*/
	}
	
	def op(operation: (Int, Int) => Unit) = {
  	  	Await.result(startThread(OpSignal(operation)), Duration.Inf).asInstanceOf[TaskResult] match {
  			case TaskResult(reslult) => {
  				reslult.foreach{
  					Await.result(_, Duration.Inf) match {
  				    	case _ =>
  					}
  				}
  				actorAdmin ! KillAllWorkers
  			}
  		}
  	  
	} 
		
	def each(opeartion: (Int, Int, Range) => Unit) = {
			Await.result(startThread(EachSignal(allRange, opeartion)), Duration.Inf).asInstanceOf[TaskResult] match {
  			case TaskResult(reslult) => {
  				reslult.foreach{
  					Await.result(_, Duration.Inf) match {
  				    	case _ =>
  					}
  				}
  				actorAdmin ! KillAllWorkers
  			}
  		}
	}
	  
	def parallel(mp: ScalaMp.type) = this
	
	def parallel_for(range: Range) = {
		allRange = Some(range)
		this
	}
	
	def withThread(num: Int) = {
		threadNum = Some(num)
		this
	}
	
  def startThread(opeartion: Operation) = actorAdmin ? StartThread(threadNum, opeartion)

  
}

class ActorAdmin extends Actor {
	
	val child = context.actorOf(Props[ActorAdmin], "mychild")
	
	val workers_pool = scala.collection.mutable.Map[Int, ActorRef]()
	val temp_workers = scala.collection.mutable.Map[Int, ActorRef]()
	
	val MAX_THREAD_NUM = 100
	
	implicit val timeout = Timeout(100 days)

	var done_sender: ActorRef = null
	
	def receive = {
	  
	  case  CriticalRegion(region) => {
		  region()
		 // sender ! Done
	  }
	  
	  case InitThreads => {
		  for(i <- 0 until MAX_THREAD_NUM) {
			  workers_pool += i -> context.actorOf(Props(new Worker(i)))
		  }
	  }
	  
	  case StartThread(threadNum, operation) => threadNum match {
		    case Some(num) => {
		    	if(num >= 0) {
		    		if(num > 0){
		    			val numm = if(num > MAX_THREAD_NUM) MAX_THREAD_NUM else num 
		    			for(i <- 0 until numm) {
		    				//val worker = context.actorOf(Props(new Worker(i)))
		    				//workers += i -> worker
		    				temp_workers += i -> workers_pool(i)
		    			}
		    		}
		    		////////////////////////////
		    		
		    		 operation match {
					    case OpSignal(op) => {
					    	val threadNum = temp_workers.size
			
					    	if(threadNum > 0){
					    		val result = temp_workers.map{case (id, worker) => worker ? OperationTask(threadNum, op)}
					    		sender ! TaskResult(result)
					    	}else child ! OpTask(op)
					    }
					    case EachSignal(range, op) => {
					    	val threadNum = temp_workers.size
					    	if(threadNum > 0){
					    		val result = child ? Calculate(range, threadNum)
					    		val senderr = sender // the position is important
					    		result foreach{
					    		  case IllegalRangeException => {}
					    		  case RangeResult(range) => {
					    			  val size = temp_workers.size

					    			  val result = (0 until threadNum).map(i => {
					    				  temp_workers(i) ? RangeTask(size, range(i), op) 
					    			  })
					    			  senderr ! TaskResult(result)
					    		  }
					    		}
					    	}else child ! EachTask(range, op)
					    }
		    		 }
		    		
		    		////////////////////////////
		    	} else if(num < 0) sender ! IllegalThreadNumException
		    }
		    case None => sender ! IllegalThreadNumException
	  }
	  
	  case EachTask(range, op) => range match {
	    	case Some(r) => op(0, 1, r)
	    	case None => // do nothing 
	  } 
	  
	  case OpTask(op) => op(0, 1)
	  
	  case Calculate(range, threadNum) => range match {
	    	case Some(r) => {
	    		val size = r.size
	    		val chushu = size / threadNum
	    		val yushu = size % threadNum
	    		val step = chushu + 1
	    		val left = step * yushu
	    		
	    		
	    		val result = (IndexedSeq[IndexedSeq[Any]]() /: (0 until threadNum)) { (acc, i) =>
	    			if(i < yushu) {
	    				val temp = i * step
	    				acc :+ (r drop temp take step)
	    			} else {
	    				acc :+ (r drop (left + (i - yushu) * chushu) take chushu)
	    			} 
	    		}    			 
	    		sender ! RangeResult(result.toList.asInstanceOf[List[Range]])
	    	} 
	    	case None => sender ! IllegalRangeException
	  }
	  case KillAllWorkers => {
		 /*val size = temp_workers.size
		 for(i <- 0 until size){
			 val worker = temp_workers.remove(i).get
			 context.stop(worker)
		 }*/
		 temp_workers.clear 
		 //println("\n------------------- Done ----------------------\n")
		 //sender ! Done
	  }
 	  case _ =>
	}
  
}

object ActorAdmin {
	  trait Operation
	  case class StartThread(threadNum: Option[Int], opeartion: Operation)
	  case class OpSignal(op: (Int, Int) => Unit) extends Operation
	  case class EachSignal(range: Option[Range], op: (Int, Int, Range) => Unit) extends Operation
	 
	  case class OpTask(op: (Int, Int) => Unit)
	  case class EachTask(range: Option[Range], op: (Int, Int, Range) => Unit)
	  case class TaskResult(futures: Iterable[Future[Any]])
	  
	  case class Calculate(range: Option[Range], threadNum: Int)
	  case class RangeResult(ranges: List[Range])
	  case class CriticalRegion(region: () => Unit)
	 	  
	  case object IllegalRangeException
	  case object IllegalThreadNumException
	  case object StartThreadSuccess
	  case object InitThreads
	  case object KillAllWorkers
}

object Worker {
  	case class OperationTask(threadNum: Int, op: (Int, Int) => Unit)
  	case class RangeTask(thradNum: Int, range: Range, op: (Int, Int,Range) => Unit)
  	case object Done
  	
  	
}

class Worker(id: Int) extends Actor {
  
	val my_rank = id
  
	def receive = {
	  case OperationTask(threadNum, op) => {
		  op(id, threadNum)
		  sender ! Done 
	  }
	  case RangeTask(threadNum, range, op) => {
		  op(id, threadNum, range)
		  sender ! Done
	  }
	  case _ =>
	}
  
}




