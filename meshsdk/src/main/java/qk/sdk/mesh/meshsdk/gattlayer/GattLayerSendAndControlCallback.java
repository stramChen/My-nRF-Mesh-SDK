package qk.sdk.mesh.meshsdk.gattlayer;

public abstract class GattLayerSendAndControlCallback {

	public void onDataLengthChanged(final int length) {

	}

	/** 
	 * Callback indicating gatt layer data send ok or not.
     *
     * @param status	 	the status code
     */
	public void onProvOut(final boolean status) {
    }

	/**
	 * Callback indicating gatt layer data send ok or not.
	 *
	 * @param status	 	the status code
	 */
	public void onProxyOut(final boolean status) {
	}

	/**
	 * Callback indicating gatt layer a data receive.
	 *
	 * @param data	 	the receive data
	 */
	public void onProvIn(final byte[] data) {
	}

	/**
	 * Callback indicating gatt layer a data receive.
	 *
	 * @param data	 	the receive data
	 */
	public void onProxyIn(final byte[] data) {
	}

}
