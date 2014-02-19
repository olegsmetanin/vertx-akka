package sw.infrastructure

import spray.http.{HttpEntity}
import spray.http.MediaTypes._
import spray.http.HttpResponse

object JSONResponse {

  def result(msg: String) = s"""
{
  "result":"$msg"
}
"""

  def error(msg: String) = s"""
{
  "error":"true",
  "result": "$msg"
}
"""

  val NOWORKERS = error("No workers available")

  val TIMEOUT = error("Request timeout")

}


object JSONHTTPResponse {
  //http://www.restapitutorial.com/httpstatuscodes.html
  //http://developer.yahoo.com/social/rest_api_guide/http-response-codes.html


  def simple(msg: String) = HttpResponse(status = 200, entity = HttpEntity(`application/json`,
    JSONResponse.result(msg)
  ))

  def error(msg: String, code: Int) = HttpResponse(status = code, entity = HttpEntity(`application/json`,
    JSONResponse.error(msg)
  ))

  val NOWORKERS = error("No workers available", 503)

  val TIMEOUT = error("Request timeout", 504)

}