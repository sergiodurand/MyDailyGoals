package com.bkpmind.mydailygoals;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Vibrator;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class CalendarActivity extends AppCompatActivity {

    private int selectedItem = -1;

    public void setSelectedItem(int i) {
        this.selectedItem = i;
    }

    public int getSelectedItem() {
        return this.selectedItem;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // Setup database
        //this.deleteDatabase("mydailygoals.db");  // debug
        final SQLiteDatabase myDb = openOrCreateDatabase("mydailygoals.db", MODE_PRIVATE, null);
        myDb.execSQL("CREATE TABLE IF NOT EXISTS goals (goal_date INT, color INT, label VARCHAR(10))");

        // Set current month label
        final TextView textMonth = (TextView) findViewById(R.id.textMonth);
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat month_date = new SimpleDateFormat("MMMM (yyyy)");
        textMonth.setText(month_date.format(cal.getTime()));

        // Configure CompactCalendarView
        final CompactCalendarView compactCalendarView = (CompactCalendarView) findViewById(R.id.compactcalendar_view);
        compactCalendarView.setFirstDayOfWeek(Calendar.SUNDAY);
        compactCalendarView.setUseThreeLetterAbbreviation(true);

        // Create the Goals list
        final ListView goals = (ListView) findViewById(R.id.goalsList);
        final String[] values = new String[] { "Goal 1 (RED)", "Goal 2 (BLUE)", "Goal 3 (GREEN)"};
        final ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < values.length; ++i) {
            list.add(values[i]);
        }
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        goals.setAdapter(adapter);
        goals.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });
        goals.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                return false;
            }
        });

        goals.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                setSelectedItem(i);
            }
        });

        // Get the first and last day from current month
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date firstDayOfMonth = cal.getTime();
        cal.add(Calendar.DATE, cal.getActualMaximum(Calendar.DAY_OF_MONTH)-1);
        Date lastDayOfMonth = cal.getTime();

        // Retrieve from database all goals from current month (between first and last day)
        Cursor myCursor = myDb.rawQuery("SELECT goal_date, color FROM goals WHERE goal_date BETWEEN ? AND ?", new String[]{String.valueOf(firstDayOfMonth.getTime()),String.valueOf(lastDayOfMonth.getTime())});
        while(myCursor.moveToNext()) {
            compactCalendarView.addEvent(new Event(myCursor.getInt(1), new Date(myCursor.getLong(0)).getTime()), false);
        }
        myCursor.close();

        compactCalendarView.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            // TODO: check if selected goal is already added to avoid duplicated goals
            @Override
            public void onDayClick(Date dateClicked) {
                if (getSelectedItem() == -1) {
                    Toast.makeText(CalendarActivity.this, "Select a goal first", Toast.LENGTH_SHORT).show();
                    return;
                }
                int color;
                ContentValues new_goal = new ContentValues();
                new_goal.put("goal_date", dateClicked.getTime());
                switch (getSelectedItem()) {
                    case 0:
                        color = Color.RED;
                        new_goal.put("label", "RED");
                        break;
                    case 1:
                        color = Color.BLUE;
                        new_goal.put("label", "BLUE");
                        break;
                    case 2:
                        color = Color.GREEN;
                        new_goal.put("label", "GREEN");
                        break;
                    default:
                        color = Color.BLACK;
                        new_goal.put("label", "BLACK");
                }
                new_goal.put("color", color);
                myDb.insert("goals", null, new_goal);
                compactCalendarView.addEvent(new Event(color, dateClicked.getTime()), false);
                Toast.makeText(CalendarActivity.this, "Goal added", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDayLongClick(Date dateClicked) {
                //TODO: remove only selected goal. Remove all goals, delete it from DB, then refresh calendar to re-add other goals
                if (getSelectedItem() == -1) {
                    Toast.makeText(CalendarActivity.this, "Select a goal first", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (myDb.delete("goals", "goal_date = ?", new String[]{String.valueOf(dateClicked.getTime())}) > 0) {
                    compactCalendarView.removeEvents(dateClicked);
                    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibe.vibrate(20);
                    Toast.makeText(CalendarActivity.this, "Goal removed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
                // Clear all goals
                compactCalendarView.removeAllEvents();

                // Get new month and set Month text
                Calendar cal = Calendar.getInstance();
                cal.setTime(firstDayOfNewMonth);
                SimpleDateFormat month_date = new SimpleDateFormat("MMMM (yyyy)");
                textMonth.setText(month_date.format(cal.getTime()));

                // Get the last day from current month
                cal.add(Calendar.DATE, cal.getActualMaximum(Calendar.DAY_OF_MONTH)-1);
                Date lastDayOfNewMonth = cal.getTime();

                // Retrieve from database all goals from current month (between first and last day)
                Cursor myCursor = myDb.rawQuery("SELECT goal_date, color FROM goals WHERE goal_date BETWEEN ? AND ?", new String[]{String.valueOf(firstDayOfNewMonth.getTime()),String.valueOf(lastDayOfNewMonth.getTime())});
                while(myCursor.moveToNext()) {
                    compactCalendarView.addEvent(new Event(myCursor.getInt(1), new Date(myCursor.getLong(0)).getTime()), false);
                }
                myCursor.close();
            }
        });
    }
}
