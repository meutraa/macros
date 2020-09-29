package host.lost.macros;

import android.app.Activity;  
import android.os.Bundle;  
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.lang.Runnable;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.content.Context;
import android.content.ContentValues;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.BaseAdapter;
import android.database.DataSetObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MainActivity extends Activity {

	private class FoodEntry {
		long id;
		String name;
		float calories;
		float fat;
		float protein;
		float carb;
		float saturated;
		float sugar;
		float fibre;
		String time;
	}

    private static class FoodView extends View {
        private static TextPaint paint;
        private static TextPaint endPaint;

        private FoodEntry f;

        FoodView(Context context, ExecutorService ex) {
            super(context);
            if (null == paint) {
                paint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
                paint.setTextSize(22);
                endPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
                endPaint.setTextAlign(Paint.Align.RIGHT);
                endPaint.setTextSize(22);
            }
        }

        void setFoodEntry(FoodEntry entry) {
            f = entry;
            postInvalidate();
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawText(f.name, 32, 32, paint);
            canvas.drawText(String.valueOf(f.carb), 32, 76, paint);
            canvas.drawText(f.time, getWidth() - 32, 32, endPaint);
            canvas.drawText(String.valueOf(f.calories), getWidth() - 32, 76, endPaint);
        }
    }

	private class FoodAdapter extends BaseAdapter {
        List<FoodEntry> entries = new ArrayList<>();

		@Override
		public View getView(int position, View cv, ViewGroup container) {
            var entry = (FoodEntry) getItem(position);
			if (null != cv) {
                var v = (FoodView) cv;
                v.setFoodEntry(entry);
			    return v;
            } else {
                var v = new FoodView(container.getContext(), ex);
                v.setLayoutParams(new FrameLayout.LayoutParams(-1, 100));
                v.setFoodEntry(entry);
			    return v;
            }
		}

		public void setItems(List<FoodEntry> e) {
            entries.clear();
            entries.addAll(e);
			notifyDataSetChanged();
		};

        public void addItem(FoodEntry e) {
            entries.add(e);
            notifyDataSetChanged();
        };

		@Override
		public int getCount() {
			return entries.size();
		}

		@Override
		public long getItemId(int p) {
			return p;
		}

		@Override
		public Object getItem(int p) {
			return entries.get(p);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}
	}

	public class FoodDbHelper extends SQLiteOpenHelper {
    	public static final int DATABASE_VERSION = 3;

    	public FoodDbHelper(Context context) {
        	super(context, "foods.db", null, DATABASE_VERSION);
    	}

    	public void onCreate(SQLiteDatabase db) {
        	db.execSQL("CREATE TABLE foods (" + 
			"id INTEGER PRIMARY KEY," +
			"name TEXT NOT NULL," +
			"calories REAL NOT NULL," +
			"fat REAL NOT NULL," +
			"carb REAL NOT NULL," +
			"saturated REAL NOT NULL," +
			"sugar REAL NOT NULL," +
			"fibre REAL NOT NULL," +
			"time INTEGER NOT NULL," +
			"protein REAL NOT NULL)"
		    );
    	}

    	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	db.execSQL("DROP TABLE IF EXISTS foods");
        	onCreate(db);
    	}

    	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	onUpgrade(db, oldVersion, newVersion);
    	}
	}

	FoodAdapter adapter;
    ListView list;
    Button button;
    FoodDbHelper helper;
    ExecutorService ex = Executors.newWorkStealingPool();

	@Override  
    protected void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  

        var frame = new FrameLayout(this);
        var bp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
		list = new ListView(this);
        button = new Button(this);
        button.setText("Add Item");
        button.setLayoutParams(bp);
        frame.addView(list);
        frame.addView(button);
		setContentView(frame);
	}

	@Override
	protected void onStart() {
		super.onStart();

		helper = new FoodDbHelper(this);
		adapter = new FoodAdapter();
		list.setAdapter(adapter);

        button.setOnClickListener(view -> {
			ex.execute(() -> {
				var db = helper.getWritableDatabase();
				var v = new ContentValues();
				v.put("name", "Test Name");
				v.put("calories", 100.5f);
				v.put("fat", 32.2f);
				v.put("protein", 16.7f);
				v.put("fibre", 16.7f);
				v.put("carb", 16.7f);
				v.put("saturated", 16.7f);
				v.put("sugar", 16.7f);
                v.put("time", System.currentTimeMillis());
		
				var id = db.insert("foods", null, v);
  				var e = new FoodEntry();
				e.id = id;
				e.name = "Test Name";
				e.carb = 100.5f;
				e.fat = 32.2f;
				e.saturated = 16.7f;
				e.protein = 16.7f;
				e.sugar = 16.7f;
				e.fibre = 16.7f;
				e.calories = 16.7f;
                e.time = (new Date()).toString();
				runOnUiThread(() -> adapter.addItem(e));
			});
        });

		ex.execute(() -> {
			var db = helper.getReadableDatabase();
			var c = db.query("foods", null, null, null, null, null, "id DESC");
			var entries = new ArrayList<FoodEntry>(c.getCount());
			while(c.moveToNext()) {
  				var e = new FoodEntry();
				e.id = c.getInt(c.getColumnIndex("id"));
				e.name = c.getString(c.getColumnIndex("name"));
				e.calories = c.getFloat(c.getColumnIndex("calories"));
				e.fat = c.getFloat(c.getColumnIndex("fat"));
				e.protein = c.getFloat(c.getColumnIndex("protein"));
				e.fibre = c.getFloat(c.getColumnIndex("fibre"));
				e.sugar = c.getFloat(c.getColumnIndex("sugar"));
				e.saturated = c.getFloat(c.getColumnIndex("saturated"));
				e.carb = c.getFloat(c.getColumnIndex("carb"));
				e.time = new Date(c.getLong(c.getColumnIndex("time"))).toString();
  				entries.add(e);
			}
			c.close();
			runOnUiThread(() -> adapter.setItems(entries));
		});
    }
}
