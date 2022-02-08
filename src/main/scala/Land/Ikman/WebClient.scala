package Land.Ikman

import com.ning.http.client.AsyncHttpClient

import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.language.postfixOps

case class BadStatus(status: Int) extends RuntimeException
object WebClient {
  private val client = new AsyncHttpClient

  def get(url: String)(implicit exec: ExecutionContextExecutor): Future[String] ={
    val f = client.prepareGet(url).execute();
    val p = Promise[String]()

    f.addListener(new Runnable {
      def run = {
        val response = f.get
        if (response.getStatusCode < 400)
          p.success(response.getResponseBody)
        else p.failure(BadStatus(response.getStatusCode))
      }
    }, exec)
    p.future
  }
  def shutdown()={
    client.close()
  }

}