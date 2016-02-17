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
