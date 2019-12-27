
# 前言：project内包含面对android、RN的MeshSDK接口，具体使用方法参考appModule的api调用方式
## 依赖引用：参考app/build.gradle

# RN调用步骤：
## 1.初始化：在第一次使用的时候调用，只需调用一次
```
ToApplication.kt ——> MeshSDK.init(context)
```
## 2.开始扫描未配对设备：type参数值见RN接口文档。需和stopScan()成对使用，scanResultCallback、errCallback都会被多次回调
```
ScanTestActivity.startScan() ——> MeshSDK.startScan(type: String, scanResultCallback: ArrayMapCallback, errCallback: IntCallback)
回调返回值见RN接口文档
```
## 3.停止扫描
```kotlin
ScanTestActivity.stopScan() ——> MeshSDK.stopScan()
```
## 4.创建NetworkKey :在开始provision之前必须保证已创建NetWorkKey，一个NetworkKey能绑定多个ApplicationKey
```kotlin
ScanTestActivity.createNetworkKey() ——> MeshSDK.createNetworkKey(networkKey: String)
```
## 5.设置当前正在使用的NetWorkKey
```kotlin
ScanTestActivity.createApplicationKey() ——> MeshSDK.createApplicationKey(netKey)
```
## 6.创建ApplicationKey：参数为NetworkKey，一个ApplicationKey只能对应绑定一个NetworkKey。在开始provision之前必须保证已创建ApplicationKey
```kotlin
ScanTestActivity.setCurrentNetworkKey() ——> MeshSDK.setCurrentNetworkKey(netKey)
```
## 7.开始provision：callback只会回调一次
```kotlin
ScanTestActivity.onItemClick ——> MeshSDK.provision(mac: String, callback: MapCallback)
回调返回值见RN接口文档
```
## 8.获取要绑定的appkey：获取指定networkKey下的所有appKey。callback只会回调一次。
```kotlin
ConnectTestActivity.getAllApplicationKey() ——> MeshSDK.getAllApplicationKey(networkKey: String, callback: ArrayStringCallback)
回调返回值见RN接口文档
```
## 9.绑定appkey:此方法会自动为该节点下的所有model绑定appkey。callback只会回调一次。
```kotlin
ConnectTestActivity.bindApplicationKeyForNode() ——> bindApplicationKeyForNode(uuid: String, appKey: String, callback: MapCallback)
``` 
## 断开连接


# Android 调用步骤
# 1. 开始扫描未配对节点

## 1.1 检查是否开启权限（蓝牙、定位）：

```kotlin
	MeshHelper.checkPermission(activity:Activity,listener:ListenerWrapper.PermissionRequestListener)
```

回调监听：
```kotlin	
ListenerWrapper.PermissionRequestListener{
	//开启权限成功，开始扫描
	override fun permissionGranted(p0:Int){ }
	//权限开启失败
	override fun permissionDenied(p0:Int){ }
	override fun permissionRationale(p0:Int){ }
}
```

## 1.2 开始扫描未配对节点：

```kotlin
MeshHelper.startScan(filterUuid:UUID,scanCallback:ScanCallback)
```

filterUuid：传 `BleMeshManager.MESH_PROVISIONING_UUID`

## 1.3.开始配对（建立provisioning连接）：

```kotlin
MeshHelper.connect(context:Context,device:ExtendedBluetoothDevice, connectToNetwork:Boolean,callback:ConnectCallback?)
```

在完成provisioning过程之后，下层会主动断开provisioning连接，此时会通过onConnectStateChange(msg:CallbackMsg)回调ConnectState.DISCONNECTED，在此回调消息之后开始建立proxy连接。

# 2.将设备绑定到家庭中

## 2.1 开始扫描已配对节点：

```kotlin
MeshHelper.startScan(filterUuid:UUID,scanCallback:ScanCallback)
```
	
filterUuid：传 `BleMeshManager.MESH_PROXY_UUID`
在 `onScanResult( )` 中判断扫描到的节点是否就是当前选中节点（通过mac地址判断）。

## 2.2 建立proxy连接：

```kotlin
MeshHelper.connect(context:Context,device:ExtendedBluetoothDevice, connectToNetwork:Boolean,callback:ConnectCallback?)
```

在 `onConnect()` 回调中提供绑定 `appKey` 操作。

注意：`onConnect()`会被多次回调。

## 2.3 添加appkey：

```kotlin
MeshHelper.addAppKeys(meshCallback:MeshCallback)
```

在 `onReceive(msg:MeshMessage)` 回调中通过参数 `ConfigAppKeyStatus.isSuccessful` 判断是否添加成功，若成功则可开始绑定appkey。

## 2.4 绑定appkey：

```kotlin
MeshHelper.bindAppKey(meshCallback:MeshCallback)
```

在 `onReceive(msg:MeshMessage)` 回调中通过参数 `ConfigModelAppStatus.isSuccessful` 判断是否绑定成功，若成功则可开始操作model。

注意：由于实际场景是一个操作界面可能会聚合多个 `model` 的功能，所以最好在初次操作设备的时候遍历为一个元素的多个 `model` 绑定`appkey`。

当一个 `model` 已经成功绑定过 `appkey` 后，本地会缓存这些信息，后续只要建立 `proxy` 连接即可直接操作 `model` 功能。

# 3.操作model功能
	
## 3.1 设置选中节点

```kotlin
MeshHelper.setSelectedMeshNode(node:ProvisionedMeshNode)
```

## 3.2 设置选中元素和model：参考

```kotlin
MainMeshActivity.bindModel()
```

在bindmodel之后即可绑定appkey，操作参考2.4。

## 3.3 操作model

发送 `MeshMessage`。其中 `GenericOnOffModel`、`VenderModel` 已经将 `opCode` 等参数固定，可直接传相应的 `MeshMessage` 实例来进行通信；同时也提供直接传原始参数接口（待定义）。

# 4. 错误码定义
```
 错误码说明：int 类型
 SDK全局错误码：100起，如未初始化SDK的错误码为101,未释放蓝牙资源133
 PROVISION错误码：200起
```

```kotlin
enum class ConnectState(var msg: String, var code: Int = 1000) {
        SDK_NOT_INIT("SDK_NOT_INIT_MSG", 101),//未初始化sdk
        CANNOT_FIND_DEVICE_BY_MAC("找不到mac地址对应的设备", 102),//传参错误，找不到mac地址对应的设备
        CONNECT_FAILED("未释放蓝牙资源", 133),//未释放蓝牙资源,android native层定义的错误码

        PROVISION_SUCCESS("provisioned", 200),//provision 成功

        CONNECTING("连接中", 201),
        DISCOVERING_SERVICE("检查服务中", 202),
        INITIALIZING("初始化蓝牙连接", 203),
        DEVICE_READY("连接初始化完成", 204),
        RECONNETCING("正在重连", 205),
        DISCONNECTING("正在断开连接", 206),
        DISCONNECTED("连接已断开", 207),
    }
```

