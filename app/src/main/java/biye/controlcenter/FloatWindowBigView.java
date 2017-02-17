package biye.controlcenter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import biye.controlcenter.ArcSeekBar.CenterPoint;
import biye.controlcenter.ArcSeekBar.OnSeekMoveListener;
import biye.controlcenter.utils.DensityUtils;
import biye.controlcenter.utils.MyWindowManager;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class FloatWindowBigView extends RelativeLayout {

	/**
	 * 记录大悬浮窗的宽度
	 */
	public static int viewWidth;

	/**
	 * 记录大悬浮窗的高度
	 */
	public static int viewHeight;

	// 记录状态栏的高度
	public static int statusBarHeight;

	// 记录当前收支位置在屏幕上的横坐标值
	private float xInScreen;

	// 记录当前手指位置在屏幕上的纵坐标值
	private float yInScreen;

	// 记录当前手指按下时再屏幕上的横坐标值
	private float xDownInScreen;

	// 记录当前手指按下时在屏幕上的纵坐标值
	private float yDownInScreen;

	// 记录手指按下时在小悬浮窗的view上的横坐标值
	private float xInView;

	// 记录手指按下时在小悬浮窗的view上的纵坐标值
	private float yInView;

	// 最内侧语音按钮
	private Button yuyin;

	// 音量seekbar上的三个指示
	private int r_voice = 230;
	private float angle_voice = (float) Math.PI / (2 * 2);
	private Button voice_plus;
	private Button voice_minus;
	private Button voice_token;

	// 亮度seekbar上的两个指示
	private int r_light = 265;
	private float angle_light = (float) Math.PI / 2;
	private Button light_token1;
	private Button light_token2;

	// 内侧三个音乐相关的按钮的半径及角度
	private int r1 = 100;
	private float angle = (float) Math.PI / (2 * 2);
	// 定义三个音乐播放器相关按钮
	private Button forward_btn;
	private Button play_btn;
	private Button backward_btn;

	// 外侧四个按钮的半径及角度
	private int r2 = 170;
	private float angle2 = (float) Math.PI / (2 * 3);
	// 定义三个音乐播放器相关按钮
	private Button rotate_btn;
	private Button gprs_btn;
	private Button wifi_btn;
	private Button voice_btn;

	// 定义两个seekbar
	private ArcSeekBar voice_seekbar;
	private ArcSeekBar light_seekbar;

	// 定义float_window_big中RelativeLayout的view
	private View view;

	// 音量相关参数定义
	private AudioManager audioManager;
	private float maxVolume, currentVolume;
	private float minVolume = 0;

	// 亮度相关参数定义
	private int screenLight;

	// 播放器相关参数
	public static final String SERVICECMD = "com.android.music.musicservicecommand";
	public static final String CMDNAME = "command";
	public static final String CMDTOGGLEPAUSE = "togglepause";
	public static final String CMDSTOP = "stop";
	public static final String CMDPAUSE = "pause";
	public static final String CMDPREVIOUS = "previous";
	public static final String CMDNEXT = "next";

	private static MediaPlayer mediaPlayer;
	File[] files;
	static int index = 0;
	public static String PATH = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + "/控制中心音乐库";

	// 记录音乐播放器包名
	public String musicPackageName;

	// 声明网络连接相关的操作变量
	private ConnectivityManager cm;

	// gprs相关参数声明
	Method setMobileDataEnabl;

	// wifi参数声明
	WifiManager wifiManager;

	private Context context;

	public FloatWindowBigView(final Context context) {
		super(context);
		this.context = context;
		LayoutInflater.from(context).inflate(R.layout.float_window_big, this);
		view = findViewById(R.id.big_window_layout);
		viewWidth = view.getLayoutParams().width;
		viewHeight = view.getLayoutParams().height;

		// 遍寻目录下音乐文件
		File pathfile = new File(PATH);
		if (!pathfile.exists()) {
			pathfile.mkdir();
		}
		if (pathfile.isDirectory()) {
			files = pathfile.listFiles();
		}

		// 在初始化中实例化网络操作类
		cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		screenLight = System.getInt(context.getContentResolver(),
				System.SCREEN_BRIGHTNESS, -1);

		// 最内侧的语音按钮，没有实现功能，暂做返回按钮
		yuyin = (Button) findViewById(R.id.yuyin);
		// seekbar上的指示初始化
		voice_plus = (Button) findViewById(R.id.voice_plus);
		voice_minus = (Button) findViewById(R.id.voice_minus);
		voice_token = (Button) findViewById(R.id.voice_token);
		light_token1 = (Button) findViewById(R.id.light_token1);
		light_token2 = (Button) findViewById(R.id.light_token2);
		// 三个音乐控制按钮的初始化
		forward_btn = (Button) findViewById(R.id.forward);
		play_btn = (Button) findViewById(R.id.play);
		backward_btn = (Button) findViewById(R.id.backward);
		// 外侧四个功能按钮的初始化
		rotate_btn = (Button) findViewById(R.id.rotate);
		gprs_btn = (Button) findViewById(R.id.gprs);
		wifi_btn = (Button) findViewById(R.id.wifi);
		voice_btn = (Button) findViewById(R.id.voice);

		// 初始化voice_seekbar,并布局voice_seekbar
		voice_seekbar = new ArcSeekBar(getContext());
		// 内侧的seekbar
		setSeekBarParams(voice_seekbar, 250, 900, 900);

		// 初始化light_seekbar，并布局light_seekbar
		light_seekbar = new ArcSeekBar(getContext());
		// 外侧seekbar
		setSeekBarParams(light_seekbar, 280, 1000, 1000);

		// 音量seekbar上的指示标志
		setBtnParams(voice_plus, 0, DensityUtils.dp2px(context, r_voice),
				angle_voice);
		setBtnParams(voice_token, 1, DensityUtils.dp2px(context, r_voice),
				angle_voice);
		setBtnParams(voice_minus, 2, DensityUtils.dp2px(context, r_voice),
				angle_voice);
		setBtnParams(light_token1, 0, DensityUtils.dp2px(context, r_light),
				angle_light);
		setBtnParams(light_token2, 1, DensityUtils.dp2px(context, r_light),
				angle_light);

		// 对内侧三个音乐控制按钮实现布局
		setBtnParams(forward_btn, 0, DensityUtils.dp2px(context, r1), angle);
		setBtnParams(play_btn, 1, DensityUtils.dp2px(context, r1), angle);
		setBtnParams(backward_btn, 2, DensityUtils.dp2px(context, r1), angle);

		// 外侧四个功能按钮的布局
		setBtnParams(voice_btn, 0, DensityUtils.dp2px(context, r2), angle2);
		setBtnParams(gprs_btn, 1, DensityUtils.dp2px(context, r2), angle2);
		setBtnParams(wifi_btn, 2, DensityUtils.dp2px(context, r2), angle2);
		setBtnParams(rotate_btn, 3, DensityUtils.dp2px(context, r2), angle2);

		// 状态初始化
		init_gprs();
		init_wifi();
		init_voice();
		init_rotate();
		init_play();

		// seekbar 声音监听事件
		voice_seekbar.setOnSeekMoveListener(new OnSeekMoveListener() {

			@Override
			public void onMove(float f) {
				currentVolume = f * maxVolume;
				audioManager.setStreamVolume(AudioManager.STREAM_RING,
						(int) (currentVolume / 100), 0);

			}
		});

		// seekbar 亮度监听事件
		light_seekbar.setOnSeekMoveListener(new OnSeekMoveListener() {

			@Override
			public void onMove(float f) {
				screenLight = (int) (f * 255 / 100);
				Settings.System.putInt(context.getContentResolver(),
						Settings.System.SCREEN_BRIGHTNESS, screenLight);
			}
		});

		yuyin.setOnClickListener(listener);

		play_btn.setOnClickListener(listener);

		forward_btn.setOnClickListener(listener);

		backward_btn.setOnClickListener(listener);

		rotate_btn.setOnClickListener(listener);

		wifi_btn.setOnClickListener(listener);

		gprs_btn.setOnClickListener(listener);

		voice_btn.setOnClickListener(listener);

	}

	// 设置两个seekbar的布局属性
	public void setSeekBarParams(ArcSeekBar seekbar, int r, int width,
								 int height) {
		ArcSeekBar.CenterPoint centerPoint_light = new CenterPoint();
		centerPoint_light.x = viewWidth;
		centerPoint_light.y = viewHeight;
		seekbar.setCenterPoint(centerPoint_light);
		seekbar.setArcColorBackground(Color.GRAY);
		seekbar.setArcStrokeWidth(2, true);
		seekbar.setArcShaperColorBackground(Color.RED);
		seekbar.setArc_radius(DensityUtils.dp2px(context, r));
		seekbar.setDotCircleRadius(20);
		seekbar.setDotColorBackground(Color.GREEN);
		if (seekbar == voice_seekbar) {
			// 音量相关参数初始化
			audioManager = (AudioManager) getContext().getSystemService(
					Context.AUDIO_SERVICE);
			currentVolume = audioManager
					.getStreamVolume(AudioManager.STREAM_RING);
			maxVolume = audioManager
					.getStreamMaxVolume(AudioManager.STREAM_RING);
			seekbar.setDotPosition(100 * currentVolume / maxVolume);
		} else if (seekbar == light_seekbar) {
			seekbar.setDotPosition(100 * screenLight / 255);
		}
		seekbar.doDraw();
		seekbar.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
		((RelativeLayout) view).addView(seekbar);
	}

	// 设置7个按钮的布局属性,以及音量，亮度指示标志的布局属性
	/*
	 * n:从右上到左下，依次为0，1，2，3 r：当前按钮所在弧线的半径 angle：当前按钮与所在弧线上所有按钮平分90°的角度
	 */
	public void setBtnParams(Button btn, int n, int r, float angle) {
		((RelativeLayout.LayoutParams) btn.getLayoutParams()).rightMargin = (int) (r * Math
				.sin(n * angle));
		((RelativeLayout.LayoutParams) btn.getLayoutParams()).bottomMargin = (int) (r * Math
				.cos(n * angle));
		((RelativeLayout.LayoutParams) btn.getLayoutParams())
				.addRule(ALIGN_PARENT_RIGHT);
		((RelativeLayout.LayoutParams) btn.getLayoutParams())
				.addRule(ALIGN_PARENT_BOTTOM);
	}

	// 检查gprs是否打开
	public boolean gprsIsOpen(String methodName) {
		Class cmClass = cm.getClass();
		Class[] argClasses = null;
		Object[] argObject = null;
		Boolean isOpen = false;
		try {
			Method method = cmClass.getMethod(methodName, argClasses);
			isOpen = (Boolean) method.invoke(cm, argObject);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return isOpen;
	}

	private void init_gprs() {
		try {
			if (gprsIsOpen("getMobileDataEnabled")) {
				gprs_btn.setBackgroundResource(R.drawable.gprs_open);
			} else {
				gprs_btn.setBackgroundResource(R.drawable.gprs);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void init_wifi() {
		if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
			wifi_btn.setBackgroundResource(R.drawable.wifi_open);
		} else {
			wifi_btn.setBackgroundResource(R.drawable.wifi);
		}
	}

	private void init_voice() {
		if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
			voice_btn.setBackgroundResource(R.drawable.voice_off);
		} else {
			voice_btn.setBackgroundResource(R.drawable.voice_on);
		}
	}

	private void init_rotate() {
		int flag = Settings.System.getInt(context.getContentResolver(),
				Settings.System.ACCELEROMETER_ROTATION, 0);
		if (flag == 0) {
			rotate_btn.setBackgroundResource(R.drawable.rotate_off);
		} else {
			rotate_btn.setBackgroundResource(R.drawable.rotate_on);
		}
	}

	private void init_play() {
		if (audioManager.isMusicActive()) {
			play_btn.setBackgroundResource(R.drawable.pause);
		} else {
			play_btn.setBackgroundResource(R.drawable.play);
		}
	}

	OnClickListener listener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.yuyin:
					// 关闭大悬浮窗，创建小悬浮窗
					MyWindowManager.removeBigWindow(context);
					MyWindowManager.createSmallWindow(context);
					break;
				case R.id.voice:
					if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
						audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
						voice_btn.setBackgroundResource(R.drawable.voice_on);
					} else {
						audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
						voice_btn.setBackgroundResource(R.drawable.voice_off);
					}
					break;

				case R.id.gprs:

					try {
						setMobileDataEnabl = cm.getClass().getDeclaredMethod(
								"setMobileDataEnabled", boolean.class);
						if (gprsIsOpen("getMobileDataEnabled")) {
							setMobileDataEnabl.invoke(cm, false);
							gprs_btn.setBackgroundResource(R.drawable.gprs);
						} else {
							setMobileDataEnabl.invoke(cm, true);
							gprs_btn.setBackgroundResource(R.drawable.gprs_open);
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
					break;

				case R.id.wifi:
					if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
						wifiManager.setWifiEnabled(false);
						wifi_btn.setBackgroundResource(R.drawable.wifi);
					} else {
						wifiManager.setWifiEnabled(true);
						wifi_btn.setBackgroundResource(R.drawable.wifi_open);
					}
					break;

				case R.id.rotate:
					int flag = Settings.System.getInt(context.getContentResolver(),
							Settings.System.ACCELEROMETER_ROTATION, 0);
					if (flag == 0) {
						Settings.System.putInt(context.getContentResolver(),
								Settings.System.ACCELEROMETER_ROTATION, 1);
						rotate_btn.setBackgroundResource(R.drawable.rotate_on);
					} else {
						Settings.System.putInt(context.getContentResolver(),
								Settings.System.ACCELEROMETER_ROTATION, 0);
						rotate_btn.setBackgroundResource(R.drawable.rotate_off);
					}
					break;

				case R.id.play:
					if (audioManager.isMusicActive()) {
						play_btn.setBackgroundResource(R.drawable.play);
					} else {
						play_btn.setBackgroundResource(R.drawable.pause);
						// startMusic();
						// Intent intent = new Intent();
						// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						// intent.setAction("android.intent.action.MUSIC_PLAYER");
						// context.startActivity(intent);

						// Intent intent = new Intent();
						// intent.setAction("com.android.music.MediaPlaybackService");
						// context.startService(intent);
						PackageManager pm = context.getPackageManager();
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.addCategory(Intent.CATEGORY_DEFAULT);
						intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.setDataAndType(Uri.fromFile(new File("")), "audio/*");
						List<ResolveInfo> mResolveInfoList = pm
								.queryIntentActivities(intent,
										PackageManager.MATCH_DEFAULT_ONLY);
						for (ResolveInfo ri : mResolveInfoList) {

							Log.d("TAG", "Icon: " + ri.loadIcon(pm));
							Log.d("TAG", "应用名: " + ri.loadLabel(pm));

							Log.i("包名: ", ri.activityInfo.packageName);
						}
					}

					// Intent intent_p = new Intent(SERVICECMD);
					// intent_p.putExtra(CMDNAME, CMDTOGGLEPAUSE);
					// context.sendBroadcast(intent_p);
					play();

					break;

				case R.id.backward:
					// Intent intent_pre = new Intent(SERVICECMD);
					// intent_pre.putExtra(CMDNAME, CMDPREVIOUS);
					// context.sendBroadcast(intent_pre);
					previous();
					break;

				case R.id.forward:
					// Intent intent_b = new Intent(SERVICECMD);
					// intent_b.putExtra(CMDNAME, CMDNEXT);
					// context.sendBroadcast(intent_b);
					next();
					break;

			}
		}
	};

	private void startMusic() {
		List<PackageInfo> packages = context.getPackageManager()
				.getInstalledPackages(0);
		for (PackageInfo info : packages) {
			Log.i("install package", info.packageName.toString());
			if (info.packageName.toString().contains("music")) {
				Intent intent = new Intent();
				intent = context.getPackageManager().getLaunchIntentForPackage(
						info.packageName.toString());
				musicPackageName = info.packageName.toString();
				if ((intent != null) && (!isRunning())) {
					context.startActivity(intent);
				}

			}
		}
	}

	private boolean isRunning() {
		ActivityManager activityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> infos = activityManager
				.getRunningAppProcesses();
		for (RunningAppProcessInfo info : infos) {
			if (info.processName.equals(musicPackageName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Log.i("key down", "fanhui anniu ");
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				// 手指按下时记录必要数据,纵坐标的值都需要减去状态栏高度
				xInView = event.getX();
				yInView = event.getY();
				xDownInScreen = event.getRawX();
				yDownInScreen = event.getRawY() - getStatusBarHeight();
				xInScreen = event.getRawX();
				yInScreen = event.getRawY() - getStatusBarHeight();
				break;
			case MotionEvent.ACTION_MOVE:
				xInScreen = event.getRawX();
				yInScreen = event.getRawY() - getStatusBarHeight();
				// 手指移动的时候更新小悬浮窗的位置
				// updateViewPosition();
				break;
			case MotionEvent.ACTION_UP:
				if ((xDownInScreen <= xInScreen && yDownInScreen <= yInScreen)) {
					// 关闭大悬浮窗，创建小悬浮窗
					MyWindowManager.removeBigWindow(context);
					MyWindowManager.createSmallWindow(context);
				}
				break;
			default:
				break;
		}
		return true;
	}

	/**
	 * 用于获取状态栏的高度。
	 *
	 * @return 返回状态栏高度的像素值。
	 */
	public int getStatusBarHeight() {
		if (statusBarHeight == 0) {
			try {
				Class<?> c = Class.forName("com.android.internal.R$dimen");
				Object o = c.newInstance();
				Field field = c.getField("status_bar_height");
				int x = (Integer) field.get(o);
				statusBarHeight = getResources().getDimensionPixelSize(x);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return statusBarHeight;
	}

	public void play() {
		if (audioManager.isMusicActive()) {
			mediaPlayer.pause();
		} else {
			if (files.length == 0) {
				Toast.makeText(context, "请向音乐库中添加音乐", Toast.LENGTH_LONG).show();
			} else {
				mediaPlayer = new MediaPlayer();
				try {
					mediaPlayer.setDataSource(files[index].getAbsolutePath());
					mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
					mediaPlayer.prepare();
					mediaPlayer.start();
					mediaPlayer
							.setOnCompletionListener(new OnCompletionListener() {

								@Override
								public void onCompletion(MediaPlayer mp) {
									next();
								}
							});
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

	}

	public void previous() {
		if (index <= 0) {
			index = files.length - 1;
		} else {
			index--;
		}
		mediaPlayer.reset();
		try {
			mediaPlayer.setDataSource(files[index].getAbsolutePath());
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.prepare();
			mediaPlayer.start();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void next() {
		if ((index < files.length - 1) && (index >= 0)) {
			index++;
		} else {
			index = 0;
		}
		mediaPlayer.reset();
		try {
			mediaPlayer.setDataSource(files[index].getAbsolutePath());
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.prepare();
			mediaPlayer.start();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
