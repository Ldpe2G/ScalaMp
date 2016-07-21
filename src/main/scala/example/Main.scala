package example


import parallelframework.ScalaMp
import parallelframework.ScalaMp._
import parallelframework._

object Main {
  
  def main(args: Array[String]): Unit = {
     // hello world	
     ScalaMp parallel withThread(8) op{ (my_rank, threadNum) =>
	     println(s"hello world , my_rank: $my_rank, threadNum: $threadNum")
     }
     println
//    
//    //schedule test
//    //default static schedule
//    println("default static schedule: ")
//    ScalaMp parallel_for(0 until 16, Default_Schecule_Static) withThread(4) each{ (my_rank, threadNum, range) =>
//        range.foreach(x => println(s"my_rank: $my_rank, index: $x"))
//    }
//    println
//    //custom static schedule
//    println("custom static schedule: ")
//    ScalaMp parallel_for(0 until 16, static(2)) withThread(4) each{ (my_rank, threadNum, range) =>
//        range.foreach(x => println(s"my_rank: $my_rank, index: $x"))
//    }
//    println
//    //default dynamic schedule
//    println("default dynamic schedule: ")
//    ScalaMp parallel_for(0 until 16, Default_Schecule_Dynamic) withThread(4) each{ (my_rank, threadNum, range) =>
//        range.foreach(x => println(s"my_rank: $my_rank, index: $x"))
//    }
//    println
//    //custom dynamic schedule
//    println("custom dynamic schedule: ")
//    ScalaMp parallel_for(0 until 16, dynamic(2)) withThread(4) each{ (my_rank, threadNum, range) =>
//        range.foreach(x => println(s"my_rank: $my_rank, index: $x"))
//    }
//    println
    
    
    /////////////////////////////////////////////////////////
    // other test
    ////////////////////////////////////////////////////////
    
    // example one
    // 梯形积分法 ,  Trapezoidal integration method
    println("Trapezoidal integration method: ")
    println("parallel version: ")
    var global = 0.0
    val a = 0.0
    val b = 20.0
    val n = 10000000
    val h = (b - a) / n
    def f(x: Double) = x * x 
    var start = System.currentTimeMillis
    
    ScalaMp parallel_for(0 to n - 1, Default_Schecule_Static) withThread(100) each{ (my_rank, threadNum, range) =>
      	val cal_range = range.map(a + _ * h) 
      	val temp = (0.0 /: cal_range){ (acc, elem) => acc + f(elem) }
      	val size = range.size
      	val local_result = {
      	     val tr = temp - (f(cal_range(0)) + f(cal_range(size - 1))) / 2
      	     tr * h
      	}
      	critical {
      	     global += local_result  
      	}  	
    }
    println(s"area = $global")
    println(s"parallel Time: ${System.currentTimeMillis - start} ms\n")

    println("serial version: ")
    start = System.currentTimeMillis
    val newRange = (0 until n).map(a + _ * h)
    val ttemp = (0.0 /: newRange){ (acc, elem) => acc + f(elem) }
    val result = {
	     val tr = ttemp - (f(newRange(0)) + f(newRange(n - 1))) / 2
	     tr * h
    }
    println(s"area = $result")
    println(s"serial Time: ${System.currentTimeMillis - start} ms")
    
    
    /*
    // example two
    // 计算 pi 值  calculate the pi
    println("calculate the pi: ")
    println("parallel version: ")
    var start = System.currentTimeMillis
    var pi = 0.0
    val n = 100000000
    ScalaMp parallel_for(0 until n, Default_Schecule_Static) withThread(100) each{ (my_rank, threadNum, range) =>
    	var factor = if(range(0) % 2 == 0) 1.0 else -1.0
    	val local_result = (0.0 /: range){ (acc, elem) =>
  	  	val temp = factor / (2 * elem + 1) 
  	  	factor = -factor
  	  	acc + temp
     	}
  	critical{ 
  	        pi += local_result
  	}
    }
    println(s"pi = ${pi * 4.0}")
    println(s"parallel Time: ${System.currentTimeMillis - start} ms\n")
		  
    println("serial version: ")
    start = System.currentTimeMillis
    val range = 0 until n
    var factor = if(range(0) % 2 == 0) 1.0 else -1.0
    val result = (0.0 /: range){ (acc, elem) =>
	   val temp = factor / (2 * elem + 1) 
	   factor = -factor
	   acc + temp
    }
    println(s"pi = ${result * 4.0}")
    println(s"serial Time: ${System.currentTimeMillis - start} ms\n")
    */
    /**
    // example three
	
    // 矩阵向量乘法   matrix vector multiplication
    println("matrix vector multiplication: ")
    println("parallel version: ")
    val n = 10000
    val row = {for(i <- 1 to n) yield 1 }.toList
    val matrix = (List[List[Int]]() /: (1 to n)){ (acc, elem) =>
	    acc :+ row
    }
    val vector = row
    val result = new Array[Int](n)
			
    var start = System.currentTimeMillis
    ScalaMp parallel_for(0 until n, Default_Schecule_Static) withThread(10) each{ (my_rank, threadNum, range) =>
	 for(index <- range) {
		 val temp = matrix(index).zip(vector).map{
		   case (l, r) => l * r
		 }.sum
		 result(index) = temp
	 }
    }
    println(s"parallel Time: ${System.currentTimeMillis - start} ms\n")
		
    println("serial version: ")
    val result2 = new Array[Int](n)
    start = System.currentTimeMillis
    for(index <- (0 until n)) {
	 val temp = matrix(index).zip(vector).map{
		  case (l, r) => l * r
	 }.sum
	  result2(index) = temp
     }
     println(s"serial Time: ${System.currentTimeMillis - start} ms\n")
		 */
		
		/*
    // example four
    //多线程下载文件   multi-thread download file 
    println("multi-thread download file: ")
    println("parallel version: ")
    import java.net._
    import java.io._
    import java.io.RandomAccessFile;
    val url = new URL("http://yinyueshiting.baidu.com/data2/music/5140129/20572571421424061128.mp3?xcode=974dbf2923e1208ffe561ce0b05f51646b547126504ae00a")
    val connection = url.openConnection.asInstanceOf[HttpURLConnection];
    connection setRequestMethod "GET"
    connection setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:17.0) Gecko/20100101 Firefox/17.0")
    connection setAllowUserInteraction true
   
    val length = connection.getContentLength
    println(s"content-length: $length")
    val file = new File("F:\\情歌王_P.mp3")
    CreateFile.createFile(file, length.toLong)
    
    var start = System.currentTimeMillis
    ScalaMp parallel_for(0 until length, Default_Schecule_Static) withThread(10) each{ (my_rank, threadNum, range) =>
    val fos = new RandomAccessFile(file, "rw");                            
    val BUFFER_SIZE = 256
    val buf = new Array[Byte](BUFFER_SIZE);    
      
    var startPos = range(0)
    var endPos = startPos + range.length - 1
    var curPos = startPos
      
     val connection2 = url.openConnection.asInstanceOf[HttpURLConnection];
      connection2 setRequestMethod "GET"
      connection2 setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT)")
      connection2 setAllowUserInteraction true
      connection2.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);//设置获取资源数据的范围，从startPos到endPos
      fos.seek(startPos);    
      val bis = new BufferedInputStream(connection2.getInputStream());                    
      while (curPos < endPos) {
          val len = bis.read(buf, 0, BUFFER_SIZE);                
          fos.write(buf, 0, len);
          curPos = curPos + len;
       }
    }
    println(s"parallel Time: ${System.currentTimeMillis - start} ms\n")
    
     println("serial version: ")
     val file2 = "F:\\情歌王_S.mp3"
    if(connection.getResponseCode == 200) {
        val out = new java.io.FileWriter(file2)
        val in = connection.getInputStream
        // 1K的数据缓冲  
        val bs = new Array[Byte](1024)
        // 读取到的数据长度  
        var len = 0
      
        val sf = new File(file2)
  
        val os = new FileOutputStream(sf)
        // 开始读取 
        len = in.read(bs)
        while(len != -1){  
          os.write(bs, 0, len)
          len = in.read(bs)
        }  
        // 完毕，关闭所有链接  
        os.close
        in.close
    }
    println(s"serial Time: ${System.currentTimeMillis - start} ms\n")
    */
		
    /**
    // example five
    //	n 皇后问题  有  bug,  parallel n queens problem
    println("parallel n queens problem: ")
    println("parallel version: ")
    val n_size = 10
    var count = 0
    var start = System.currentTimeMillis
    ScalaMp parallel withThread(n_size) op{ (my_rank, threadNum) =>
	   val serach = List(my_rank)
	   val result = placeQueens(n_size, n_size, serach)
	   critical{
	  	 count += result.size
	   }
    }
    println(s"solutions found: $count")
    println(s"parallel Time: ${System.currentTimeMillis - start} ms\n")
	   
    println("serial version: ")
    start = System.currentTimeMillis
    count = 0
    for(i <- 0 until n_size){
        val r = placeQueens(n_size, n_size, List(i))
        //r.foreach(l => println(show(l)))
        count += r.size
    }
    println(s"solutions found: $count")
    println(s"serial Time: ${System.currentTimeMillis - start} ms\n")
    */
//    ScalaMp.Terminate()
  }
  
   //把List对应的一个解转换成String表示形式
   def show(queens: List[Int]) = {
  	 val queens_s = queens.length
	 val lines =
		  for ( col <- queens.reverse )
		        yield Vector.fill(queens_s)("* ").updated(col, "^ ").mkString
	 s"\n${lines mkString "\n"}"
    }    
		
    def placeQueens(k: Int, n: Int, searchList: List[Int]): Set[List[Int]] = {
            if( k == 1 ) Set(searchList)
            else
            for{
                queens <- placeQueens(k - 1, n, searchList)
                col <- 0 until n
                if isSafe(col, queens)
             } yield col :: queens
    }
     
   
    //判断在col该位置能否放一个皇后
    def isSafe(col: Int, queens: List[Int]) = {
	        val row = queens.length
	        //将每一行和对应的皇后的位置放在一个原组中
	        val queensWithRow = (row - 1 to 0 by -1) zip queens
	        val b = queensWithRow forall {
	            //col不能和存在的列重合，还有行差和列差不能相等，防止对角线重合
	            case (r,c) => col != c 
	        }
	    	b && math.abs(col - queens(0)) != row - queensWithRow(0)._1 
     }   
	  
}

