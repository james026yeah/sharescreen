package com.archermind.ashare.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.Handler;
import android.os.Message;
import android.util.Log;


public final class WiRemoteCmdClient {
	private final static String TAG = "WiRemoteCmdClient";
	private final static int SERVER_PORT = 9999;
	private final static int MSG_ON_MESSAGE_ARRIVED = 0;
	private final static int MSG_ON_SERVER_CONNECTED = 1;
	private static WiRemoteCmdClient sInstance = null;
	private ClientThread mThread = null;
	private boolean mIsStarted = false;
	private Handler mHandler = null;
	private onWiRemoteCmdListener mListener = null;
	private Handler.Callback mCb = new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_ON_MESSAGE_ARRIVED:
				onMessageArrived((String)msg.obj);
				break;
			case MSG_ON_SERVER_CONNECTED:
				if(mListener != null) {
					mListener.onServerConnected();
				}
				break;
			default:
				break;
			}
			return false;
		}		
	};
	
	public void setOnWiRemoteCmdListener(onWiRemoteCmdListener ls) {
		mListener = ls;
	}
	
	public interface onWiRemoteCmdListener {
		public void onServerConnected();
		public void onServerConnectError(int errorId);
	}
	
	private void onMessageArrived(String message) {
		WiRemoteCmdParser cmdParser = new WiRemoteCmdParser(message);
		Log.v(TAG, "on received message:" + message);
		if(mListener == null) return;
		int command = cmdParser.getInt(Commands.KEY_CMD, Commands.CMD_INVALID);
		switch(command) {
		case Commands.CMD_SERVER_CONNECT_ERROR:
			mListener.onServerConnectError(
					cmdParser.getInt(Commands.KEY_SERVER_CONNECT_ERROR_ID, Commands.INVALID_ERROR_ID));
			break;
		default:
			break;
		}
	}

	public boolean isServerConnected() {
		return (mThread != null) ? mThread.isServerConnected() : false;
	}
	public void start(String serverIP) {
		Log.v(TAG, "invoke start >>>>>>>>>>>>>>>>>>>>>>>>>");
		if(!mIsStarted) {
			Log.v(TAG, "start the client >>>>>>>>>>>>>>>>>>>>>>>>>");
			prepare(serverIP);
			mIsStarted = true;
		}
	}
	public void stop() {
		Log.v(TAG, "invoke stop <<<<<<<<<<<<<<<<<<<<<<<<<<");
		if(mIsStarted) {
			Log.v(TAG, " stop the client <<<<<<<<<<<<<<<<<<<<<<<<<<");
			//mThread.interrupt();
			mThread.quit();
			mThread = null;
			mIsStarted = false;
		}
	}
	private void prepare(String serverIP) {
		mThread = new ClientThread(serverIP);
		mThread.start();
	}
	
	private WiRemoteCmdClient() {
		mHandler = new Handler(mCb);
	}
	
	public static WiRemoteCmdClient getInstance() {
		if(sInstance == null) {
			sInstance = new WiRemoteCmdClient();
		}
		return sInstance;
	}
	
	public boolean sendMessage(String message) {
		boolean bSend = false;
		if(mThread != null) {			
			bSend = mThread.sendMessage(message);
			Log.v(TAG, "send messge-->" + message + ", bSend:" + bSend);
		}
		return bSend;
	}
	
	protected class ClientThread extends Thread {
		private BufferedReader mReader = null;
		private PrintWriter mWriter = null;
		private String mServerIP = null;
		private Socket mSocket = null;
		public ClientThread(String serverIP) {
			mServerIP = serverIP;			
		}
		
		public boolean sendMessage(String message) {			
			boolean bSend = false;
			if (mWriter != null) {
				mWriter.println(message);
				bSend = true;
			}
			return bSend;
		}
		
		public void quit() {
			if(mSocket != null) {
				try {
					Log.v(TAG, "close socket!!");
					mSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		public boolean isServerConnected() {
			return (mSocket != null && mWriter != null && mReader != null);
		}
		
		public void run() {
			Log.v(TAG, "entering client thread --> ");
			String message = null;
			try {		
				Log.v(TAG, "server IP:" + mServerIP + ", SERVER_PORT" + SERVER_PORT);				
				mSocket = new Socket(mServerIP, SERVER_PORT);
				mReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
				mWriter = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(mSocket.getOutputStream())), true);
				
				if (mHandler != null) {
					mHandler.sendMessage(Message.obtain(null, MSG_ON_SERVER_CONNECTED, message));
				}
				while(true) {
					Log.v(TAG, "try to read from socket --> ");
					if (mSocket.isConnected() && !mSocket.isInputShutdown()) {
						message = mReader.readLine();
						Log.v(TAG, "Read Message: " + message);	
						if(message == null) {
							Log.v(TAG, "remote socket closed");
							break;
						} else {
							if (mHandler != null) {
								mHandler.sendMessage(Message.obtain(null, MSG_ON_MESSAGE_ARRIVED, message));
							}
						}
					} else {
						Log.v(TAG, "disconnected ir input shutdown, so quit!: ");
						break;
					}
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (ConnectException e){
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				if(mReader != null) {
					mReader.close();
					mReader = null;
				}
				if(mWriter != null) {
					mWriter.close();
					mWriter = null;
				}
				mSocket = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
			Log.v(TAG, "exit client thread --> ");			
		}
	};
}
