package pen.gesture.input;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class GestureKeyboard extends InputMethodService 
	implements OnGesturePerformedListener {
	
	// HashMap<Название языка, библиотека жестов>
	private HashMap<String, GestureLibrary> mGestureLibMap;
	// Текущий язык
	private String mCurrentGestureLib = null;
	
	private View mInputView;
	
	private Button mLangSwither;
	private ToggleButton mCapsLock;
	private ToggleButton mUpperLock;
	private ToggleButton mLowerLock;
	
	@Override 
	public View onCreateInputView() {
		if(getFilesDir().list().length == 0) {
			copyFromAssets(getAssets(), "09", getFilesDir(), "123");
			copyFromAssets(getAssets(), "en", getFilesDir(), "eng");
			copyFromAssets(getAssets(), "ru", getFilesDir(), "рус");
			copyFromAssets(getAssets(), "en_caps", getFilesDir(), "ENG");
			copyFromAssets(getAssets(), "ru_caps", getFilesDir(), "РУС");
		}
		
		// Загружаем слой
		mInputView = getLayoutInflater().inflate(R.layout.input, null);
		
		// Находим View рисования жестов
		GestureOverlayView gestures = (GestureOverlayView) mInputView.findViewById(R.id.gestures);
		gestures.addOnGesturePerformedListener(this);
		gestures.setGestureColor(0xFF0000FF);
		
		// Находим кнопки
		mLangSwither = (Button) mInputView.findViewById(R.id.lang);
		mCapsLock = (ToggleButton) mInputView.findViewById(R.id.caps);
		mLowerLock = (ToggleButton) mInputView.findViewById(R.id.lower);
		mUpperLock = (ToggleButton) mInputView.findViewById(R.id.upper);
		
		// Обработчик изменения состояния кнопок
		OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				onStateClick(buttonView, isChecked);
			}
		};
		
		mLowerLock.setOnCheckedChangeListener(onCheckedChangeListener);
		mUpperLock.setOnCheckedChangeListener(onCheckedChangeListener);
		mCapsLock.setOnCheckedChangeListener(onCheckedChangeListener);
		
		// При клике по кнопке настроек показываем настройки
		mInputView.findViewById(R.id.preferences).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent prefIntent = new Intent(getBaseContext(), GesturesListActivity.class);
				prefIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getApplication().startActivity(prefIntent);
			}
		});
		
		// При длинном клике показываем диалог выбора клавиатуры
		mInputView.findViewById(R.id.preferences).setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				InputMethodManager imm = (InputMethodManager)
						getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showInputMethodPicker();
				return false;
			}
		});
		
		return mInputView;
	}
	
	public void onStartInputView(EditorInfo info, boolean restarting) {
		super.onStartInputView(info, restarting);
		// Загружаем жесты
		loadLanguages();
		
		// Устанавливаем кнопке "язык" текущий язык
		mLangSwither.setText(mCurrentGestureLib);
	}
	
	private void loadLanguages() {
		// Собираем список "языков"
		File[] files = getFilesDir().listFiles();
		mGestureLibMap = new HashMap<String, GestureLibrary>();
		
		// Загружаем "языки"
		for(int i = 0; i < files.length; i++) {
			GestureLibrary gestureLib = GestureLibraries.fromFile(files[i]);
			
			if (!gestureLib.load()) {
				Toast.makeText(this, "Can't load gestures " + files[i].getAbsolutePath(), 
						Toast.LENGTH_LONG).show();
				continue;
			}
			
			// Добавляем язык в список
			mGestureLibMap.put(files[i].getName(), gestureLib);
		}
		
		if(mCurrentGestureLib == null || !mGestureLibMap.containsKey(mCurrentGestureLib)) {
			// Находим первый язык и устанавлием его
			mCurrentGestureLib = mGestureLibMap.keySet().toArray(new String[0])[0];
			if(mGestureLibMap.containsKey(mCurrentGestureLib.toLowerCase())) 
				mCurrentGestureLib = mCurrentGestureLib.toLowerCase();
		}
	}
	
	public static void copyFromAssets(AssetManager assets, String from, File dir, String to) {
		try {
			InputStream in = assets.open(from);
			OutputStream out = new FileOutputStream(new File(dir, to));
			
			byte[] buffer = new byte[1024];
			int read;
			while((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			
			in.close();
			out.close();
		} catch(Exception e) {
			Log.e("GestureKeyboard", "Exception: " + e.getMessage());
		}
	}

	public void onStateClick(CompoundButton buttonView, boolean isChecked) {
		switch(buttonView.getId()) {
		case R.id.lower:
			// Клик по кнопке lower
			if(isChecked) {
				// Отключаем upper и caps
				mUpperLock.setChecked(false);
				mCapsLock.setChecked(false);
			}
			break;
		case R.id.upper:
			// Клик по кнопке upper
			if(isChecked) {
				// Отключаем lower и caps
				mLowerLock.setChecked(false);
				mCapsLock.setChecked(false);
			}
			break;
		case R.id.caps:
			// Клик по кнопке caps
			if(isChecked) {
				mLowerLock.setChecked(false);
				
				if(mGestureLibMap.containsKey(mCurrentGestureLib.toUpperCase())) {
					// Устанавливаем язык в UpperCase
					mUpperLock.setChecked(false);
					mCurrentGestureLib = mCurrentGestureLib.toUpperCase();
				} else {
					// Если нет языка в UpperCase, то включаем UpperLock
					mUpperLock.setChecked(true);
				}
			} else {
				mUpperLock.setChecked(false);

				if(mGestureLibMap.containsKey(mCurrentGestureLib.toLowerCase())) {
					// Устанавливаем язык в LowerCase
					mLowerLock.setChecked(false);
					mCurrentGestureLib = mCurrentGestureLib.toLowerCase();
				} else {
					// Или включаем LowerLock
					mLowerLock.setChecked(true);
				}
			}
			
			// Устанавливем язык
			mLangSwither.setText(mCurrentGestureLib);
			break;
		}
	}
	
	public void onLangClick(View v) {
		// Клик по кнопке смены языка
		// Получаем список языков
		String gestureNames[] = mGestureLibMap.keySet().toArray(new String[0]);
		
		boolean next = false;
	
		for(int i = 0; i < gestureNames.length; i++) {
			if(next) {
				// Это следующий язык за текущим
				if(!mCurrentGestureLib.toLowerCase().equals(gestureNames[i].toLowerCase())) {
					// Это не текущий язык в другом регистре
					setCurrentLanguage(gestureNames, i);				
					next = false;
					break;
				}
				continue;
			}
			if(mCurrentGestureLib.toLowerCase().equals(gestureNames[i].toLowerCase())) {
				// Нашли текущий язык в списке
				next = true;
			}
		}
	
		if(next) setCurrentLanguage(gestureNames, 0);
		
		// Устанавливаем язык
		mLangSwither.setText(mCurrentGestureLib);
	}
	
	private void setCurrentLanguage(String gestureNames[], int index) {
		mCurrentGestureLib = gestureNames[index];
		
		// Если есть этот язык в LowerCase, то включаем его
		if(mGestureLibMap.containsKey(gestureNames[index].toLowerCase())) {
			mCurrentGestureLib = gestureNames[index].toLowerCase();
		}
		
		// Если включен капс, то ищем этот язык в UpperCase и включаем если есть
		if(mCapsLock.isChecked() && mGestureLibMap.containsKey(gestureNames[index].toUpperCase())) {
			mCurrentGestureLib = gestureNames[index].toUpperCase();
		}
	}
	
	private void keyDownUp(int keyEventCode) {
		// Посылаем события keyDown и keyUp
		getCurrentInputConnection().sendKeyEvent(
				new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
		getCurrentInputConnection().sendKeyEvent(
				new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
	}
	
	private String editString(String in) {
		// Если включен lower, то преобразуем в LowerCase
		if(mLowerLock.isChecked())
			return in.toLowerCase();
		// Если upper, то в UpperCase
		if(mUpperLock.isChecked())
			return in.toUpperCase();
		// Или возвращаем оригинал
		return in;
	}

	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		// Распознаём жест
		ArrayList<Prediction> predictions = mGestureLibMap.get(mCurrentGestureLib).recognize(gesture);
		
		if (predictions.size() > 0) {
			// Выбираем самый подходящий вариант
			Prediction prediction = predictions.get(0);
			
			if(prediction.name.startsWith("@@")) {
				// Если название начинаеться с @@, то посылаем нажатие кнопки с данным кодом
				try {
					keyDownUp(Integer.valueOf(prediction.name.substring(2)));
				} catch(NumberFormatException e) {
					Log.w("GestureKeyboard", "NumberFormatException: " + e.getLocalizedMessage());
				}
			} else {
				// Посылаем название жеста как текст
				getCurrentInputConnection().commitText(editString(prediction.name), 
						prediction.name.length());
			}
		}
	}
}
