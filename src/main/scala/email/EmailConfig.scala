package pdftojpg

import java.util.Properties

import com.typesafe.config.ConfigFactory

import scalaz.concurrent.Task

// TODO: Get config to use non-String Object values
case class EmailConfig(pop3: Pop3Config, smtp: SmtpConfig, rsmMessage: RsmMessageConfig, username: String, password: String, pollingPeriodSeconds: Int) {
  // TODO: Pull all from config in one shot when config is loaded.
  def sslPop3Properties: Properties = {
    val SSL_FACTORY = "javax.net.ssl.SSLSocketFactory"

    val props = new Properties()

    props.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY)
    props.setProperty("mail.pop3.socketFactory.fallback", "false")
    props.setProperty("mail.pop3.port", pop3.port.toString)
    props.setProperty("mail.pop3.socketFactory.port", pop3.port.toString)
    props
  }

  def smtpProperties: Properties = {
    val props = new Properties()
    props.setProperty("mail.smtp.auth", "true")
    props.setProperty("mail.smtp.starttls.enable", "true")
    props.setProperty("mail.smtp.host", smtp.host)
    props.setProperty("mail.smtp.port", smtp.port.toString)
    props
  }
}
case class RsmMessageConfig(toAddress: String, ccAddress: String, body: String)
case class Pop3Config(host: String, port: Int)
case class SmtpConfig(host: String, port: Int)


object EmailConfig {
  def loadConfig: Task[EmailConfig] = Task.delay {
    val config = ConfigFactory.load("application.conf")
    pureconfig.loadConfigOrThrow[EmailConfig](config, "app.email")
  }
}


