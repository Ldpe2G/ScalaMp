package example


import parallelframework.ScalaMp
import parallelframework.ScalaMp._


object Main {
  
  def main(args: Array[String]): Unit = {
    
	  	// hello world	
		ScalaMp parallel withThread(8) op{ (my_rank, threadNum) =>
		 	println(s"hello world , my_rank: $my_rank, threadNum: $threadNum")
	  }
		println
		/*
    // 梯形积分法 ,  Trapezoidal integration method
    var global = 0.0
		val a = 0.0
		val b = 20.0
		val n = 10000000
		val h = (b - a) / n
		def f(x: Double) = x * x 
		var start = System.currentTimeMillis
		ScalaMp parallel_for(0 to n - 1) withThread(100) each{ (my_rank, threadNum, range) =>
		  	val cal_range = range.map(a + _ * h) 
		  	val temp = (0.0 /: cal_range){ (acc, elem) => acc + f(elem) }
		  	val size = range.size
		  	val local_result = {
		  		val tr = temp - (f(cal_range(0)) + f(cal_range(size - 1))) / 2
		  		tr * h
		  	}
		  	critical{
		  		global += local_result  
		  	}  	
		}
		println(s"area = $global")
		println(s"parallel Time: ${System.currentTimeMillis - start}")

		start = System.currentTimeMillis
		val newRange = (0 until n).map(a + _ * h)
		val ttemp = (0.0 /: newRange){ (acc, elem) => acc + f(elem) }
		val result = {
	  		val tr = ttemp - (f(newRange(0)) + f(newRange(n - 1))) / 2
	  		tr * h
	  }
		println(s"area = $result")
		println(s"serial Time: ${System.currentTimeMillis - start}")
  	*/
    
		
		// 计算 pi 值  calculate the pi
    var start = System.currentTimeMillis
    var pi = 0.0
    val n = 100000000
    ScalaMp parallel_for(0 until n) withThread(100) each{ (my_rank, threadNum, range) =>
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
		println(s"parallel Time: ${System.currentTimeMillis - start}")
		  
		start = System.currentTimeMillis
		val range = 0 until n
		var factor = if(range(0) % 2 == 0) 1.0 else -1.0
	  	val result = (0.0 /: range){ (acc, elem) =>
	  	  	val temp = factor / (2 * elem + 1) 
	  	  	factor = -factor
	  	  	acc + temp
	  	}
		println(s"pi = ${result * 4.0}")
		println(s"serial Time: ${System.currentTimeMillis - start}")
		 
		 
		// 矩阵向量乘法   matrix vector multiplication
		/*
		val n = 10000
		val row = {for(i <- 1 to n) yield 1 }.toList
		val matrix = (List[List[Int]]() /: (1 to n)){ (acc, elem) =>
		  	acc :+ row
		}
		val vector = row
		val result = new Array[Int](n)
				
		var start = System.currentTimeMillis
		ScalaMp parallel_for(0 until n) withThread(10) each{ (my_rank, threadNum, range) =>
			for(index <- range) {
				val temp = matrix(index).zip(vector).map{
				  case (l, r) => l * r
				}.sum
				result(index) = temp
			}
		}
		println(s"parallel Time: ${System.currentTimeMillis - start}")
		
		val result2 = new Array[Int](n)
		start = System.currentTimeMillis
		for(index <- (0 until n)) {
			val temp = matrix(index).zip(vector).map{
			  case (l, r) => l * r
			}.sum
			result2(index) = temp
		}
		println(s"serial Time: ${System.currentTimeMillis - start}")
		 */
		
		
		/**
    		//多线程下载图片   multi-thread download file 
        import java.net._
        import java.io._
        import java.io.RandomAccessFile;
    		//val url = new URL("http://b.zol-img.com.cn/desk/bizhi/image/5/1920x1080/1415587376954.jpg");
    		
    		//val url = new URL("http://img.bz1111.com/d7/2014-5/2014052507522.jpg");
		    val addr = new InetSocketAddress("127.0.0.1", 8087)
        val proxy = new Proxy(Proxy.Type.HTTP, addr)
		    val url = new URL("https://r2---sn-q4f7sn7l.googlevideo.com/videoplayback?upn=QztvidPOegU&ip=107.178.200.2&mime=video/mp4&gir=yes&requiressl=yes&mm=31&mv=u&source=youtube&ms=au&signature=C28884A93FBF7B72F622789B779292B9168BBD69.D6680F0972C51F0CEE015AB0FAB1F9CD7CDA48BD&dur=327.762&sparams=clen,dur,gir,id,ip,ipbits,itag,keepalive,lmt,mime,mm,ms,mv,requiressl,source,upn,expire&fexp=900718,922247,924630,927622,930676,932404,9405714,9405794,941004,943917,945086,947209,947218,948124,952302,952605,952901,953912,955301,957103,957105,957201,958615&itag=133&ipbits=0&sver=3&lmt=1406266306254345&expire=1418841817&mt=1418820108&key=yt5&clen=9643067&keepalive=yes&id=o-ANKyeM1f_UZCXNq4raai7TILF7KLVs2ulmbdxdcxB-U4")
        val connection = url.openConnection(proxy).asInstanceOf[HttpURLConnection];
        connection setRequestMethod "GET"
        connection setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT)")
        connection setAllowUserInteraction true
       
        val length = connection.getContentLength
        println(length)
        //val file = new File("F:\\ScalaMP_P.jpg")
        //val file = new File("F:\\小苹果_P.mp3")
       
        //CreateFile.createFile(file, length.toLong)
        
        var start = System.currentTimeMillis
        ScalaMp parallel_for(0 until length) withThread(10) each{ (my_rank, threadNum, range) =>
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
        println(s"parallel Time: ${System.currentTimeMillis - start}")
        
       
        
        //start = System.currentTimeMillis
        //val file2 = "F:\\ScalaMP_S.jpg"
        val file2 = "F:\\小苹果_S.flv"
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
      println(s"serial Time: ${System.currentTimeMillis - start}")
      */
     
		
	 
	  //	n 皇后问题  有  bug,  parallel n queens problem
		/*  
	  val n_size = 6 
	  var count = 0
	  var start = System.currentTimeMillis
	  ScalaMp parallel withThread(n_size) op{ (my_rank, threadNum) =>
	  		val serach = List(my_rank)
	  		val result = placeQueens(n_size, n_size, serach)
	  		critical{
	  			count += result.size
	  		}
	  }
	  println(count)
	  println(s"parallel Time: ${System.currentTimeMillis - start}\n")
	   
	  start = System.currentTimeMillis
	  count = 0
	  for(i <- 0 until n_size){
	    val r = placeQueens(n_size, n_size, List(i))
	    //r.foreach(l => println(show(l)))
	    count += r.size
	  }
	  println(count)
	  println(s"serial Time: ${System.currentTimeMillis - start}")
	  */
    
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

