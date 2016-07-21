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
  
  //not work , program not stop after calling this function, don't know why
  def Terminate(): Unit = {
     val result = actorAdmin ? KillAllWorkers
     Await.result(result, Duration.Inf) match {
        case Done => {
          actorAdmin ! Kill
          actorSystem.terminate()
        }
     }
  }
	
  //implement the critical region
  def critical(region: => Unit): Unit = {
      actorAdmin ! CriticalRegion(() => region)
	 /*val resul = actorAdmin ? CriticalRegion(() => region)
	 Await.result(resul, Duration.Inf) match {
  		 case Done =>  println("done")      
  	 }*/
   }
	
   def op(operation: (Int, Int) => Unit): Unit = {
  	  	Await.result(startThread(OpSignal(operation)), Duration.Inf).asInstanceOf[TaskResult] match {
  			case TaskResult(reslult) => {
  				reslult.foreach{
  					Await.result(_, Duration.Inf) match {
  				    	case _ =>
  					}
  				}
  				actorAdmin ! ClearTmpWorkers
  			}
  		}
  	  
	} 
		
  def each(opeartion: (Int, Int, Range) => Unit): Unit = {
  	 Await.result(startThread(EachSignal(allRange, opeartion, schedulee)), Duration.Inf).asInstanceOf[TaskResult] match {
  			case TaskResult(reslult) => {
  				reslult.foreach{
  					Await.result(_, Duration.Inf) match {
  				    	case _ =>
  					}
  				}
  				actorAdmin ! ClearTmpWorkers
  			}
  		}
   }
	  
     def parallel(mp: ScalaMp.type): ScalaMp.type = this
	
     val DYNAMIC = -100
     implicit val Default_Schecule_Static = static(DYNAMIC)
     implicit val Default_Schecule_Dynamic = dynamic(DYNAMIC)
  
     var schedulee: schedule = Default_Schecule_Static
  
     def parallel_for(range: Range, schedule: schedule): ScalaMp.type = {
       schedulee = schedule
	     allRange = Some(range)
	     this
     }
	
     def withThread(num: Int): ScalaMp.type = {
    	 threadNum = Some(num)
    	 this
     }
	
     def startThread(opeartion: Operation): Future[Any] = actorAdmin ? StartThread(threadNum, opeartion)

  
}

trait schedule
case class static(num: Int) extends schedule
case class dynamic(num: Int) extends schedule
  

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
		     //sender ! Done
	  }
	  
	  case InitThreads => {
		  for(i <- 0 until MAX_THREAD_NUM) {
			  workers_pool += i -> context.actorOf(Props(new Worker(i)))
		  }
	  }
	  
	  case StartThread(threadNum, operation) => threadNum match {
		    case Some(num) => {
      		if(num > 0){
      			val numm = if(num > MAX_THREAD_NUM) MAX_THREAD_NUM else num 
      			for(i <- 0 until numm) {
      				temp_workers += i -> workers_pool(i)
      			}
      		}
		    	operation match {
    		      case OpSignal(op) => {
      			    	val threadNum = temp_workers.size
      	          if(threadNum == 0){
                     temp_workers += 0 -> workers_pool(0)
                  }
                  val result = temp_workers.map{case (id, worker) => worker ? OperationTask(threadNum, op)}
                  sender ! TaskResult(result)
    			    }
    			    case EachSignal(range, op, schedule) => {
                  val size = temp_workers.size
  					    	val threadNum =  if(size == 0){
                      temp_workers += 0 -> workers_pool(0)
                      1
                    } else size
                 
    				    		val result = child ? Calculate(range, threadNum, schedule)
    				    		val senderr = sender // the position is important
    				    		result foreach{
    					    		  case IllegalRangeException => {}
    					    		  case RangeResult(range) => schedule match {
                             case static(num) => {
                               	//println(range.size)
                               	val thread_num = temp_workers.size
                               	var index = 0
                               	val result =(List[Future[Any]]() /: range){ (acc, elem) =>
                                     if(index >= thread_num) index = 0
                                     val re = temp_workers(index) ? RangeTask(thread_num, elem, op) 
                                     index += 1
                        					   acc :+ re
                        					} 
                      					  senderr ! TaskResult(result)
                    					}
                    					case dynamic(num) => {
                    						 val thread_num = temp_workers.size
                    						 var index = 0
                    						 val result =(List[Future[Any]]() /: range){ (acc, elem) =>
                          						if(index >= thread_num) index = 0
                          						val re = temp_workers(index) ? RangeTask(thread_num, elem, op) 
                            						index += 1
                            						acc :+ re
                        					 } 
                        					senderr ! TaskResult(result)
                    					}
    					    		  }
    				    		}
				         }
		    		   }
		    }
		    case None => sender ! IllegalThreadNumException
	  }
	      
	  case Calculate(range, threadNum, schedule) => range match {
	    	case Some(r) => {
          val size = r.size
          val result = schedule match {  
              case static(num) => {
                  if(num <= 0){
                      val chushu = size / threadNum
                      val yushu = size % threadNum
                      val step = chushu + 1
                      val left = step * yushu
                      (IndexedSeq[IndexedSeq[Any]]() /: (0 until threadNum)) { (acc, i) =>
                          if(i < yushu) {
                              val temp = i * step
                              acc :+ (r drop temp take step)
                          } else {
                              acc :+ (r drop (left + (i - yushu) * chushu) take chushu)
                          } 
                      }  
                  }else{
                      val chushu = size / num
                      val yushu = size % num
                      
                      val tmp = 
                        for(i <- 0 until chushu)
                            yield r drop (i * num) take num
                      if(yushu != 0)  tmp :+ r drop (chushu * num) take yushu 
                      else tmp
                  }
              }  
              case dynamic(num) => {
                    val internal = if(num <= 0) 1 else num 
                    val chushu = size / internal
                    val yushu = size % internal
                    val tmp = 
                      for(i <- 0 until chushu)
                          yield r drop (i * internal) take internal
                    if(yushu != 0)  tmp :+ r drop (chushu * internal) take yushu   
                    else tmp          
              }
          }
          sender ! RangeResult(result.toList.asInstanceOf[List[Range]])
	    	} 
	    	case None => sender ! IllegalRangeException
	  }
	  case ClearTmpWorkers => temp_workers.clear
	  
    case KillAllWorkers => {
         val keys = workers_pool.keys
         for(k <- keys){
           workers_pool(k) ! Kill
         }
         child ! Kill
                 
         sender ! Done
    }
    
 	  case _ =>
	}
  
}

object ActorAdmin {
	  trait Operation
	  case class StartThread(threadNum: Option[Int], opeartion: Operation)
	  case class OpSignal(op: (Int, Int) => Unit) extends Operation
	  case class EachSignal(range: Option[Range], op: (Int, Int, Range) => Unit, schedule: schedule) extends Operation
	 
	 
	  case class TaskResult(futures: Iterable[Future[Any]])
	  
	  case class Calculate(range: Option[Range], threadNum: Int, schedule: schedule)
	  case class RangeResult(ranges: List[Range])
	  case class CriticalRegion(region: () => Unit)
	 	  
	  case object IllegalRangeException
	  case object IllegalThreadNumException
	  case object StartThreadSuccess
	  case object InitThreads
    case object ClearTmpWorkers
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
	}
  
}




