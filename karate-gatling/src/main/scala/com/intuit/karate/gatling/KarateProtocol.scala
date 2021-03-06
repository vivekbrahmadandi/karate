package com.intuit.karate.gatling

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import com.intuit.karate.core.ScenarioContext
import com.intuit.karate.http.{HttpRequestBuilder, HttpUtils}
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.{Protocol, ProtocolComponents, ProtocolKey}
import io.gatling.core.session.Session

case class MethodPause(val method: String, pause: Int)

class KarateProtocol(val uriPatterns: Map[String, Seq[MethodPause]]) extends Protocol {
  def pathMatches(uri: String): Option[String] = uriPatterns.keys.find(HttpUtils.parseUriPattern(_, uri) != null)
  def pauseFor(requestName: String, method: String) = {
    val methodPause = uriPatterns.getOrElse(requestName, Nil).find(mp => method.equalsIgnoreCase(mp.method))
    if (methodPause.isDefined) methodPause.get.pause else 0
  }
  val actorCount = new AtomicInteger()
  val defaultNameResolver = (req: HttpRequestBuilder, ctx: ScenarioContext) => {
    val pathPair = HttpUtils.parseUriIntoUrlBaseAndPath(req.getUrlAndPath)
    val matchedUri = pathMatches(pathPair.right)
    if (matchedUri.isDefined) matchedUri.get else pathPair.right
  }
  var nameResolver: (HttpRequestBuilder, ScenarioContext) => String = (req, ctx) => null
}

object KarateProtocol {
  val KarateProtocolKey = new ProtocolKey {
    type Protocol = KarateProtocol
    type Components = KarateComponents
    override def defaultProtocolValue(configuration: GatlingConfiguration) = new KarateProtocol(Map.empty)
    override def newComponents(system: ActorSystem, coreComponents: CoreComponents)=
      karateProtocol => KarateComponents(karateProtocol, system)
    override def protocolClass= classOf[KarateProtocol].asInstanceOf[Class[io.gatling.core.protocol.Protocol]]
  }
}

case class KarateComponents(val protocol: KarateProtocol, val system: ActorSystem) extends ProtocolComponents {
  def onStart: Option[Session => Session] = None
  def onExit: Option[Session => Unit] = None
}
