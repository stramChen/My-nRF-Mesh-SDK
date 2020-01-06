# BLE Mesh React Native API 需求定义

我们先定义 SDK 的名字，比如 `MeshSDK`。

## 版本
|错误码|错误信息|
|:---|:---|
|0.1.0|定义 API 需求|
|0.1.1|「对设备进行网络配置 - Bind Application Key 阶段」API 变动，增加 applicationKey 参数|
|0.1.2|添加错误码描述、修改connect接口传参、添加callback次数描述|

---- 

## SDK 的初始化配置

### 初始化 Mesh SDK，全局只需调用一次
```js
MeshSDK.init()
```

---- 

## 权限相关

### 检查蓝牙权限

```js
MeshSDK.checkPermission(callback)
```

关于 callback 的参数: String
- 已经授权 GRANTED
- 用户拒绝 DENIED
- 可能更多

---- 

## Network Key 的相关操作
```makefile
一个家庭共用一个networkKey
```

### 创建 Network Key（传入存储）

```js
MeshSDK.createNetworkKey(key: string)
```

### 删除 Network Key

```js
MeshSDK.removeNetworkKey(key: string)
```

### 列出所有的 Network Key

```js
MeshSDK.getAllNetworkKey(): Array<string>
```

### 设置当前 Network Key

```js
MeshSDK.setCurrentNetworkKey(key: string)
```

### 查询当前 Network Key

```js
MeshSDK.getCurrentNetworkKey(): string
```

---- 
## Application Key 的相关操作

### 创建 Application Key

```js
MeshSDK.createApplicationKey(networkKey: string)
```

### 删除 Application Key

```js
MeshSDK.removeApplicationKey(applicationKey: string, networkKey: string)
```

### 列出所有的 Application Key

```js
MeshSDK.getAllApplicationKey(networkKey: string): Array<string>
```

### 设置当前 Application Key

```js
MeshSDK.setCurrentApplicationKey(key: string, networkKey: string)
```

### 查询当前 Application Key

```js
MeshSDK.getCurrentApplicationKey(networkKey: string): string
```

---- 
## 蓝牙设备的扫描

### 开始扫描周围的蓝牙设备：需和stopScan成对使用。onScanResult、onError都会被多次回调

```js
MeshSDK.startScan(type: string, onScanResult: callback(Array<Map>), onError:callback(code:int))
```

type: string
- `provisioned` 已配置网络的设备
- `unProvisioned` 未配置网络的设备

onScanResult: callback(Array\<Map/Dictionary\>)
Map:
- mac: string
- rssi: int
- name: string


### 停止扫描周围的蓝牙设备

```js
MeshSDK.stopScan()
```

---- 

## 对设备进行网络配置

### 对设备进行网络配置 - Provision 阶段：callback只会回调一次

```js
MeshSDK.provision(uuid: string, callback(error: Map))
```

callback
- error
	- `{code: int, message: string}`
		- code == 200 成功
		- code != 200 失败


### 对设备进行网络配置 - Bind Application Key 阶段：callback只会回调一次。

```js
MeshSDK.bindApplicationKeyForNode(uuid: string, applicationKey: string, callback())
MeshSDK.bindApplicationKeyForBaseModel(uuid: string, applicationKey: string, callback()) // 先实现绑定两个基本的 model
```
绑定model需要一个一个顺序进行，否则会出错。

网络配置完成。

---- 
## 控制设备

### 获取本地已经配置的节点：callback只会回调一次。

```js
MeshSDK.getProvisionedNodes(callback(Array<Map>))
```

Map
- name: string
- mac: string
- elements: Array\<Map\>
	- elementAddress: int
	- models: Array\<Map\>
		- modelId: string
		- subscriptionAddresses: Array\<int\>

### 连接某一个 mesh 网络中的设备(同上)：callback会回调多次。ios无此方法，只有android需要。
```js
MeshSDK.connect(networkKey: string, callback(error: Map))
```

### 对设备发送控制指令

```js
// MeshSDK.setCurrentNode(uuid: string) // 选择当前要控制的 mesh 节点设备
MeshSDK.setGenericOnOff(uuid: string, bool, callback(success: bool)) // true: 开, false: 关
MeshSDK.setLightProperties(uuid: string, C: int, W: int, R: int, G: int, B: int, callback(success: bool))
MeshSDK.sendMeshMessage(uuid: string, element: int, model: int, opcode: string, value: string, callback(success: bool))
```

注意，调用 `setGenericOnOff ` 或 `setLightProperties ` 方法之前务必之行 `setCurrentNode `方法。
TODO: Android 消息转发应用层有概率收不到设备回复的消息，需要排查一下 @王悦


### 对设备进行重置
此方法包含 2 个步骤：
1. 向节点设备发送重置消息
2. 从 mesh 网络中移除设备

```js
MeshSDK.resetNode(mac: string)
```

---- 
## 网络的导入和导出

### 导出 mesh 网络配置

```js
MeshSDK.exportConfiguration(callback(data: JSONString))
```

### 导入 mesh 网络配置

```js
MeshSDK.importConfiguration(data: JSONString, callback(success))
```
## code定义
|错误码|错误信息|
|:---|:---|
|101|未初始化sdk|
|102|传参错误，找不到uuid对应的设备|
|103|未建立连接|
|104|未开启蓝牙|
|105|没有设置current networkKey|
|133|未释放蓝牙资源|
|200|全局通用，成功|
|201|建立连接中|
|202|检查服务中|
|203|初始化蓝牙连接|
|205|正在重连|
|206|正在断开连接|
|207|连接已断开|
|208|绑定appkey失败|
|401|netKey正在使用中，需先删除netKey对应的设备|
|402|appKey正在使用中，需先删除appKey对应的设备|
