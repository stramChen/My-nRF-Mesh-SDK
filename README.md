依赖引用：参考app/build.gradle
初始化：MeshHelper.initMesh(applicationContext)

# 1. 开始扫描未配对节点

## 1.1 检查是否开启权限（蓝牙、定位）：

```java
	MeshHelper.checkPermission(activity:Activity,listener:ListenerWrapper.PermissionRequestListener)
```

回调监听：
```java	
ListenerWrapper.PermissionRequestListener{
	//开启权限成功，开始扫描
	override fun permissionGranted(p0:Int){ }
	//权限开启失败
	override fun permissionDenied(p0:Int){ }
	override fun permissionRationale(p0:Int){ }
}
```

## 1.2 开始扫描未配对节点：

```java
MeshHelper.startScan(filterUuid:UUID,scanCallback:ScanCallback)
```

filterUuid：传 `BleMeshManager.MESH_PROVISIONING_UUID`

## 1.3.开始配对（建立provisioning连接）：

```java
MeshHelper.connect(context:Context,device:ExtendedBluetoothDevice, connectToNetwork:Boolean,callback:ConnectCallback?)
```

在完成provisioning过程之后，下层会主动断开provisioning连接，此时会通过onConnectStateChange(msg:CallbackMsg)回调ConnectState.DISCONNECTED，在此回调消息之后开始建立proxy连接。

# 2.将设备绑定到家庭中

## 2.1 开始扫描已配对节点：

```java
MeshHelper.startScan(filterUuid:UUID,scanCallback:ScanCallback)
```
	
filterUuid：传 `BleMeshManager.MESH_PROXY_UUID`
在 `onScanResult( )` 中判断扫描到的节点是否就是当前选中节点（通过mac地址判断）。

## 2.2 建立proxy连接：

```java
MeshHelper.connect(context:Context,device:ExtendedBluetoothDevice, connectToNetwork:Boolean,callback:ConnectCallback?)
```

在 `onConnect()` 回调中提供绑定 `appKey` 操作。

注意：`onConnect()`会被多次回调。

## 2.3 添加appkey：

```java
MeshHelper.addAppKeys(meshCallback:MeshCallback)
```

在 `onReceive(msg:MeshMessage)` 回调中通过参数 `ConfigAppKeyStatus.isSuccessful` 判断是否添加成功，若成功则可开始绑定appkey。

## 2.4 绑定appkey：

```java
MeshHelper.bindAppKey(meshCallback:MeshCallback)
```

在 `onReceive(msg:MeshMessage)` 回调中通过参数 `ConfigModelAppStatus.isSuccessful` 判断是否绑定成功，若成功则可开始操作model。

注意：由于实际场景是一个操作界面可能会聚合多个 `model` 的功能，所以最好在初次操作设备的时候遍历为一个元素的多个 `model` 绑定`appkey`。

当一个 `model` 已经成功绑定过 `appkey` 后，本地会缓存这些信息，后续只要建立 `proxy` 连接即可直接操作 `model` 功能。

# 3.操作model功能
	
## 3.1 设置选中节点

```java
MeshHelper.setSelectedMeshNode(node:ProvisionedMeshNode)
```

## 3.2 设置选中元素和model：参考

```java
MainMeshActivity.bindModel()
```

在bindmodel之后即可绑定appkey，操作参考2.4。

## 3.3 操作model

发送 `MeshMessage`。其中 `GenericOnOffModel`、`VenderModel` 已经将 `opCode` 等参数固定，可直接传相应的 `MeshMessage` 实例来进行通信；同时也提供直接传原始参数接口（待定义）。

