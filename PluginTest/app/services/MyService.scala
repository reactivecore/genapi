package services

import model.User
import play.api.mvc.Result
import play.api.mvc.Results

import scala.concurrent.Future

class MyService {

  def getUser(): Future[User] = {
    Future.successful(User("John Doe"))
  }

  def withoutResult(): Future[Unit] = {
    Future.successful(())
  }

  def withStringResult(): Future[String] = {
    Future.successful("Hello World")
  }

  def withResultResult(): Future[Result] = {
    Future.successful(Results.Ok("Hello World").withSession("hello" -> "world"))
  }

  def echoUser(user: User): Future[User] = {
    Future.successful(user)
  }

  def echoInt(in: Int): Future[String] = Future.successful(in.toString)

  def echoString(in: String): Future[String] = Future.successful(in)
}