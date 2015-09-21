import com.urbancode.air.AirPluginTool
import groovyx.net.http.RESTClient

def apTool = new AirPluginTool(this.args[0], this.args[1])
props = apTool.getStepProperties()
def username = props['username']
def password = props['password']

def unencodedAuthString = username + ":" + password
def bytes = unencodedAuthString.bytes
encodedAuthString = bytes.encodeBase64().toString()

println "Skytap Authentication Parameters Created"

apTool.setOutputProperty("username", username)
apTool.setOutputProperty("password", password)
apTool.setOutputProperties()
