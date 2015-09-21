import com.urbancode.air.AirPluginTool
import groovyx.net.http.RESTClient
import groovyx.net.http.ContentType

def apTool = new AirPluginTool(this.args[0], this.args[1])
props = apTool.getStepProperties()
def configID = props['configID']
def VMName = props['VMName']
def username = props['username']
def password = props['password']

def unencodedAuthString = username + ":" + password
def bytes = unencodedAuthString.bytes
encodedAuthString = bytes.encodeBase64().toString()

println "Get Virtual Machine ID Command Info:"
println "	Environment ID: " + configID
println "	VM Name: " + VMName
println "Done"

def IDTESRESTClient = new RESTClient('https://cloud.skytap.com/')
IDTESRESTClient.defaultRequestHeaders.'Authorization: Basic' = encodedAuthString
IDTESRESTClient.defaultRequestHeaders.'Accept' = "application/json"
IDTESRESTClient.defaultRequestHeaders.'Content-Type' = "application/json"

response = IDTESRESTClient.get(path: "configurations/" + configID)

vmID = 0
vmList = response.data.vms

vmList.each {
	if (it.name == VMName) {
		println "Found VM Name: " + it.name
		vmID = it.id
	}
}

if (vmID == 0) {
	System.err.println "Error: VM Name \"" + VMName + "\" not found"
	exit (1)
}
println "VM ID: " + vmID

apTool.setOutputProperty("vmID", vmID)
apTool.setOutputProperties()
