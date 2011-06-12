package pen.gesture.input;

import java.io.File;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class GesturesListActivity extends ListActivity {
	private static final int MENU_ID_RENAME = 1;
	private static final int MENU_ID_REMOVE = 2;

	private static final int DIALOG_RENAME_GESTURE = 1;
	
	private ArrayAdapter<String> mAdapter;
	private EditText mInput;
	private Dialog mRenameDialog;
	private String mCurrentRenameLanguage;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gestures_list);
		
		if(getFilesDir().list().length == 0) {
			GestureKeyboard.copyFromAssets(getAssets(), "09", getFilesDir(), "123");
			GestureKeyboard.copyFromAssets(getAssets(), "en", getFilesDir(), "eng");
			GestureKeyboard.copyFromAssets(getAssets(), "ru", getFilesDir(), "рус");
			GestureKeyboard.copyFromAssets(getAssets(), "en_caps", getFilesDir(), "ENG");
			GestureKeyboard.copyFromAssets(getAssets(), "ru_caps", getFilesDir(), "РУС");
		}
		
		// Загружаем языки (списки жестов)
		mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		setListAdapter(mAdapter);
		registerForContextMenu(getListView());
		
		getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View v, int pos, long id) {
				String item = mAdapter.getItem(pos);
				loadLanguage(item);
			}
		});
		
		loadLanguages();
		
		// Активируем кнопки
		findViewById(R.id.addButton).setEnabled(true);
		findViewById(R.id.reloadButton).setEnabled(true);
	}
	
	public void addGesture(View v) {
		// Клик по кнопке создания нового языка
		Intent intent = new Intent(this, GestureBuilderActivity.class);
		startActivity(intent);
	}
	
	public void reloadGestures(View v) {
		// Клик по кнопке "Reload" 
		loadLanguages();
	}
	
	public void loadLanguage(String name) {
		// Открытие языка
		Intent intent = new Intent(this, GestureBuilderActivity.class);
		intent.setData(Uri.parse(new File(getFilesDir(), name).getAbsolutePath()));
		startActivity(intent);
	}
	
	private void loadLanguages() {
		// Сканируем папку для файлов
		File[] files = getFilesDir().listFiles();
		
		// Очищаем список и добавлем новые языки
		mAdapter.clear();
		for(int i = 0; i < files.length; i++)
			mAdapter.add(files[i].getName());
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		cleanupRenameDialog();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		// Создаём контексное меню из двух кнопок (Rename & Remove)
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		menu.setHeaderTitle(((TextView) info.targetView).getText());
		
		menu.add(0, MENU_ID_RENAME, 0, R.string.gestures_rename);
		menu.add(0, MENU_ID_REMOVE, 0, R.string.gestures_delete);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// Определяем по какому элементу списка был клик
		final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)
		item.getMenuInfo();
		final String gesture = mAdapter.getItem(menuInfo.position);
		
		switch (item.getItemId()) {
		case MENU_ID_RENAME:
			// Вызываем диалог переименования
			renameLanguage(gesture);
			return true;
		case MENU_ID_REMOVE:
			// Удаляем язык
			deleteLanguage(gesture);
			return true;
		}
		
		return super.onContextItemSelected(item);
	}
	
	private void renameLanguage(String gesture) {
		mCurrentRenameLanguage = gesture;
		showDialog(DIALOG_RENAME_GESTURE);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_RENAME_GESTURE) {
			return createRenameDialog();
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		if (id == DIALOG_RENAME_GESTURE) {
			mInput.setText(mCurrentRenameLanguage);
		}
	}

	private Dialog createRenameDialog() {
		// Основой диалога будет макет dialog_rename
		final View layout = View.inflate(this, R.layout.dialog_rename, null);
		mInput = (EditText) layout.findViewById(R.id.name);
		((TextView) layout.findViewById(R.id.label)).setText(R.string.gestures_rename_label);

		// Собираем диалог
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(0);
		builder.setTitle(getString(R.string.gestures_rename_title));
		builder.setCancelable(true);
		builder.setOnCancelListener(new Dialog.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				cleanupRenameDialog();
			}
		});
		builder.setNegativeButton(getString(R.string.cancel_action),
			new Dialog.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					cleanupRenameDialog();
				}
			}
		);
		builder.setPositiveButton(getString(R.string.rename_action),
			new Dialog.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					changeGestureName();
				}
			}
		);
		builder.setView(layout);
		return builder.create();
	}

	private void changeGestureName() {
		// Для переименования переместим файл языка в новое имя
		new File(getFilesDir(), mCurrentRenameLanguage).renameTo(new File(getFilesDir(), 
				mInput.getText().toString()));
		
		// Перезагрузим список языков
		loadLanguages();
		mCurrentRenameLanguage = null;
	}

	private void cleanupRenameDialog() {
		// Заметаем следы за диалогом
		if (mRenameDialog != null) {
			mRenameDialog.dismiss();
			mRenameDialog = null;
		}
		mCurrentRenameLanguage = null;
	}

	private void deleteLanguage(String gesture) {
		// Удаляем язык
		new File(getFilesDir(), gesture).delete();
		loadLanguages();
		Toast.makeText(this, R.string.gestures_delete_success, Toast.LENGTH_SHORT).show();
	}
}
