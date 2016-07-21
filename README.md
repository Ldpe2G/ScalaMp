ScalaMp V1.0.1
=======

a simple parallel computing framework  inspired by openmp and  implemented in scala

Examples:
=======

1, Hello World:
```scala

            import parallelframework.ScalaMp
            import parallelframework.ScalaMp._
            
            ScalaMp parallel withThread(8) op{ (my_rank, threadNum) =>
                println(s"hello world , my_rank: $my_rank, threadNum: $threadNum")
	        }
```
###output:
```scala
            hello world , my_rank: 5, threadNum: 8
            hello world , my_rank: 0, threadNum: 8
            hello world , my_rank: 6, threadNum: 8
            hello world , my_rank: 3, threadNum: 8
            hello world , my_rank: 4, threadNum: 8
            hello world , my_rank: 7, threadNum: 8
            hello world , my_rank: 2, threadNum: 8
            hello world , my_rank: 1, threadNum: 8
```

2, calculate the pi, 计算pi值
-------
            var start = System.currentTimeMillis
            var pi = 0.0
            val n = 100000000
            ScalaMp parallel_for(0 until n, Default_Schedule_Static) withThread(100) each{ (my_rank, threadNum, range) =>
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

###output:
             
             pi = 3.141592643589774
             parallel Time: 2485



more examples are in the example.Main.scala
-------



Some limitations (future works):
=======

###1, thread num must be large than 0 and less equal than 100,  threadNum > 0 && threadNum <= 100
###2, critical section may not implemented correctly
###3, only implement the static and dynamic schedule strategies, like the schedule[] directive in openmp, my not implemented correctly
