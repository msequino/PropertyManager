import controllers.PropertyController
import org.junit.Test
import org.junit.runner.RunWith
import org.specs2.Specification
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import play.api.{Configuration, Environment}
import play.api.mvc.{ControllerComponents, DefaultControllerComponents}
import play.test.WithApplication
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind

@RunWith(classOf[JUnitRunner])
class PropertyTest extends Specification with Mockito {

  def bindings(env: Environment, conf: Configuration) = Seq(
    bind[ControllerComponents].to[DefaultControllerComponents]
  )

  /*val userDaoMock = mock[UserDaoService]
  userDaoMock.lookupUser(1207059) returns Seq()
*/
  val appMock = new GuiceApplicationBuilder()
    .overrides(bind[ControllerComponents].to(classOf[DefaultControllerComponents]))
    .build

  @Test def testGet() = new WithApplication(appMock) {

    val res = new PropertyController().get()
//    there was one(userDaoMock).lookupUser(1207059)
  }
}
