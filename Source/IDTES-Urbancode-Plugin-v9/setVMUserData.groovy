//
// Copyright 2015 Skytap Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

import com.urbancode.air.AirPluginTool
import groovyx.net.http.RESTClient
import groovyx.net.http.ContentType

def apTool = new AirPluginTool(this.args[0], this.args[1])
props = apTool.getStepProperties()
def configID = props['configID']
def VMName = props['VMName']
def userDataString = props['userDataString']
def username = props['username']
def password = props['password']

def unencodedAuthString = username + ":" + password
def bytes = unencodedAuthString.bytes
encodedAuthString = bytes.encodeBase64().toString()

println "Set Virtual Machine User Data Command Info:"
println "	Environment ID: " + configID
println "	VM Name: " + VMName
println "	User Data String: " + userDataString
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

println "VM ID: " + vmID

response = IDTESRESTClient.post(path: "configurations/" + configID + "/vms/" + vmID + "/user_data.json" ,
	body: ['contents':userDataString],
	requestContentType: ContentType.JSON)

 println "Set User Data to " + userDataString
