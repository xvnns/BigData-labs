package philosophers


import java.util.concurrent.Semaphore
import org.apache.zookeeper._
import scala.util.Random

case class Philosopher(id: Int, hostPort: String, root: String, left: Semaphore, right: Semaphore, waiting: Integer)
  extends Watcher {
  val zk = new ZooKeeper(hostPort, 3000, this)
  val mutex = new Object()
  val path: String = root + "/" + id.toString

  if (zk == null) throw new Exception("ZK is NULL.")

  override def process(event: WatchedEvent): Unit = {
    mutex.synchronized {
      mutex.notify()
    }
  }

  def eat(): Boolean = {
    printf("Философ № %d хочет поесть\n", id)
    mutex.synchronized {
      var created = false
      while (true) {
        if (!created) {
          zk.create(path, Array.emptyByteArray, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL)
          created = true
        }
        val eating = zk.getChildren(root, this)
        if (eating.size() > waiting) {
          zk.delete(path, -1)
          mutex.wait(3000)
          Thread.sleep(Random.nextInt(5)* 100)
          created = false
        } else {
          if(id == 0){
            right.acquire()
            printf("Философ № %d берет правую вилку\n", id)
            left.acquire()
            printf("Философ %d берет левую вилку\n", id)
            Thread.sleep((Random.nextInt(5) + 1) * 1000)
            right.release()
            printf("Философ № %d положил правую вилку\n", id)
            left.release()
            printf("Философ № %d положил левую вилку и закончил есть\n", id)
            return true
          } else {
            left.acquire()
            printf("Философ %d берет левую вилку\n", id)
            right.acquire()
            printf("Философ № %d берет правую вилку\n", id)
            Thread.sleep((Random.nextInt(5) + 1) * 1000)
            right.release()
            printf("Философ № %d положил правую вилку\n", id)
            left.release()
            printf("Философ № %d положил левую вилку и закончил есть\n", id)
            return true
          }
        }
      }
    }
    false
  }

  def think(): Unit = {
    printf("Философ № %d прокрастинирует\n", id)
    zk.delete(path, -1)
    Thread.sleep((Random.nextInt(5) + 1) * 1000)
  }
}
