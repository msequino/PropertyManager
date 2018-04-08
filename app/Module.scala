import com.google.inject.AbstractModule
import java.time.Clock

import controllers.PropertyController
import play.api.{Configuration, Environment}
import play.api.mvc.{AbstractController, ControllerComponents, DefaultControllerComponents}
import services._
import services.price.{PriceService, PriceServiceImpl}
import services.property.{PropertyService, PropertyServiceImpl}

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.

 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module extends AbstractModule {

  override def configure() = {
    // Use the system clock as the default implementation of Clock
    bind(classOf[ControllerComponents]).to(classOf[DefaultControllerComponents])
    bind(classOf[PropertyService]).to(classOf[PropertyServiceImpl])
    bind(classOf[PriceService]).to(classOf[PriceServiceImpl])
  }

}
