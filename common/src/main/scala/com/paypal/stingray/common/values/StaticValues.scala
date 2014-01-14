package com.paypal.stingray.common.values

import java.net.URL
import java.util.Properties
import java.io.File
import com.paypal.stingray.common.logging.LoggingSugar
import com.paypal.stingray.common.option._
import com.paypal.stingray.common.env.StingrayEnvironmentType
import com.paypal.stingray.common.constants.ValueConstants._
import scala.util.Try

/**
 * Created with IntelliJ IDEA.
 * User: drapp
 * Date: 3/13/13
 * Time: 5:50 PM
 */
class StaticValues(mbUrl: Option[URL])
  extends Values
  with LoggingSugar {

  def this(url: URL) = this(Option(url))
  def this(serviceName: String) = this(StaticValues.getServiceUrl(Some(serviceName)))
  def this() = this(StaticValues.getServiceUrl(None))

  val logger = getLogger[StaticValues]

  private lazy val props: Option[Properties] = {
    lazy val p = new Properties
    for {
      url <- mbUrl
      stream <- Try(url.openStream()).toOption
    } yield {
      p.load(stream)
      p
    }
  }

  lazy val stingrayEnvType = getEnum[StingrayEnvironmentType](StingrayEnvironment)
    .orThrow(lookupFailed(StingrayEnvironment))
  lazy val isDev: Boolean = stingrayEnvType == StingrayEnvironmentType.DEVELOPMENT
  lazy val isStaging: Boolean = stingrayEnvType == StingrayEnvironmentType.STAGING
  lazy val isProd: Boolean = stingrayEnvType == StingrayEnvironmentType.PRODUCTION

  /**
   * Only use for mission critical things where we simply can't function without
   * a real vaue and there's no default. Logs and throws a scary error if the
   * value isn't there.
   * @param key the key to get
   * @return
   */
  def getOrDie(key: String): String = {
    get(key).orThrow(lookupFailed(key))
  }

  def getIntOrDie(key: String): Int = {
    getInt(key).orThrow(lookupFailed(key))
  }

  def getLongOrDie(key: String): Long = {
    getLong(key).orThrow(lookupFailed(key))
  }

  private def lookupFailed(key: String) = {
    val msg = "Failed to lookup mission critical values from property files %s!!!!!!".format(key)
    logger.error(msg)
    new IllegalStateException(msg)
  }

  def get(key: String): Option[String] = props.flatMap(p => Option(p.getProperty(key)))
}

object StaticValues {
  type Identity[X] = X

  lazy val defaultValues = new StaticValues()

  def getServiceUrl(serviceName: Option[String]): Option[URL] = {
    Try(
      serviceName.flatMap(s => Option(System.getProperty("%s.config".format(s))).map(new File(_).toURI.toURL)) orElse
        serviceName.flatMap(s => Option(getClass.getClassLoader.getResource("%s.properties".format(s)))) orElse
        serviceName.flatMap(s => Option(getClass.getResource("%s-default.properties".format(s)))) orElse
        Option(System.getProperty("stingray.cluster.config")).map(new File(_).toURI.toURL)
    ).toOption.flatten
  }

}
