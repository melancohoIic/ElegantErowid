package com.melancholiclabs.eleganterowid;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.melancholiclabs.eleganterowid.model.IndexItem;
import com.melancholiclabs.eleganterowid.model.VaultIndexItem;

import java.util.ArrayList;
import java.util.Scanner;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * Created by Melancoholic on 7/4/2016.
 */
public class ReportFragment extends Fragment {

    private static final String TAG = ReportFragment.class.getName();

    private static final ErowidDB EROWID_DB = ErowidDB.getInstance();

    private static final String ARG_TITLE = "title";
    private static final String ARG_AUTHOR = "author";
    private static final String ARG_SUBSTANCE = "substance";
    private static final String ARG_DATE = "date";
    private static final String ARG_REPORT_URL = "reportUrl";

    private static final String ARG_NAME = "name";
    private static final String ARG_URL = "url";
    private static final String ARG_INDEX_TYPE = "indexType";

    private String mTitle;
    private String mAuthor;
    private String mSubstance;
    private String mDate;
    private String mReportUrl;

    private String mName;
    private String mUrl;
    private String mIndexType;

    private IndexItem mSubstanceMatch;

    private LinearLayout mLinearLayout;
    private MaterialProgressBar mProgressBar;

    private Menu mMenu;

    private LoadReportPageTask mLoadReportPageTask;
    private FetchMatchingSubstanceTask mFetchMatchingSubstanceTask;

    public ReportFragment() {
    }

    public static ReportFragment newInstance(VaultIndexItem item, String name, String url, String indexType) {
        ReportFragment fragment = new ReportFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, item.getTitle());
        args.putString(ARG_AUTHOR, item.getAuthor());
        args.putString(ARG_SUBSTANCE, item.getSubstance());
        args.putString(ARG_DATE, item.getDate());
        args.putString(ARG_REPORT_URL, item.getUrl());
        args.putString(ARG_NAME, name);
        args.putString(ARG_URL, url);
        args.putString(ARG_INDEX_TYPE, indexType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mTitle = getArguments().getString(ARG_TITLE);
        mAuthor = getArguments().getString(ARG_AUTHOR);
        mSubstance = getArguments().getString(ARG_SUBSTANCE);
        mDate = getArguments().getString(ARG_DATE);
        mReportUrl = getArguments().getString(ARG_REPORT_URL);
        mName = getArguments().getString(ARG_NAME);
        mUrl = getArguments().getString(ARG_URL);
        mIndexType = getArguments().getString(ARG_INDEX_TYPE);

        mLoadReportPageTask = new LoadReportPageTask();
        mLoadReportPageTask.execute();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_substance, container, false);

        mLinearLayout = (LinearLayout) v.findViewById(R.id.substance_linear_layout);
        mProgressBar = (MaterialProgressBar) v.findViewById(R.id.substance_progress_bar);

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.mMenu = menu;
        inflater.inflate(R.menu.menu_substance, menu);

        MenuItem item = mMenu.findItem(R.id.action_substance_vault);
        item.setIcon(getResources().getDrawable(R.drawable.ic_board));
        item.getIcon().setAlpha(130);
        item.setCheckable(false);

        mFetchMatchingSubstanceTask = new FetchMatchingSubstanceTask();
        mFetchMatchingSubstanceTask.execute();
    }

    private void showSubstanceOption() {
        Log.d(TAG, "showSubstanceOption is called");
        MenuItem item = mMenu.findItem(R.id.action_substance_vault);
        item.getIcon().setAlpha(255);
        item.setCheckable(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_substance_vault) {
            Log.d(TAG, "Action \"substance_vault\" was selected");
            if (item.isCheckable()) {
                Intent intent = SubstanceActivity.newIntent(getContext(), mSubstanceMatch);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Elegant Erowid could not match this vault item to a substance item.", Toast.LENGTH_SHORT).show();
            }
            // TODO overridePendingAnimation
            return true;
        }

        if (id == R.id.action_substance_open_in_browser) {
            Log.d(TAG, "Action \"open_in_browser\" was selected");
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mReportUrl));
            startActivity(browserIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStop() {
        super.onStop();
        Utils.gracefullyStop(mLoadReportPageTask);
        Utils.gracefullyStop(mFetchMatchingSubstanceTask);
    }

    /**
     * Creates a Title TextView and Paragraph TextView then adds them to the LinearLayout.
     *
     * @param label title text and log tag
     * @param text  paragraph text
     */
    private void addSection(final String label, final String text) {
        TextView titleTextView = (TextView) getLayoutInflater(null).inflate(R.layout.title_text_view, null);
        titleTextView.setText(label);

        TextView paragraphTextView = (TextView) getLayoutInflater(null).inflate(R.layout.paragraph_text_view, null);
        paragraphTextView.setText(text);
        paragraphTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                copyTextToClipboard(label, text);
                return true;
            }
        });

        mLinearLayout.addView(titleTextView);
        mLinearLayout.addView(paragraphTextView);
    }

    /**
     * Creates a Title TextView and Paragraph TextView then adds them to the LinearLayout.
     *
     * @param label title text and log tag
     * @param text  paragraph text
     */
    private void addSection(final String label, final Spanned text) {
        TextView titleTextView = (TextView) getLayoutInflater(null).inflate(R.layout.title_text_view, null);
        titleTextView.setText(label);

        TextView paragraphTextView = (TextView) getLayoutInflater(null).inflate(R.layout.paragraph_text_view, null);
        paragraphTextView.setText(text);
        paragraphTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                copyTextToClipboard(label, text.toString());
                return true;
            }
        });

        mLinearLayout.addView(titleTextView);
        mLinearLayout.addView(paragraphTextView);
    }

    /**
     * Creates a TableLayout from a given string and adds it to the LinearLayout.
     *
     * @param table string to parse into a TableLayout
     */
    private void addTable(String label, String table) {
        TextView titleTextView = (TextView) getLayoutInflater(null).inflate(R.layout.title_text_view, null);
        titleTextView.setText(label);

        Context context = getContext();
        if (context == null) return;

        TableLayout tableLayout = new TableLayout(context);
        tableLayout.setShrinkAllColumns(true);

        Scanner scanner = new Scanner(table);
        ArrayList<TableRow> rows = new ArrayList<>();
        while (scanner.hasNextLine()) {
            TableRow tableRow = new TableRow(context);
            tableRow.setGravity(Gravity.CENTER_HORIZONTAL);
            tableRow.setBackgroundColor(Color.LTGRAY);

            Scanner lineScanner = new Scanner(scanner.nextLine());
            lineScanner.useDelimiter("\t");
            ArrayList<TextView> columns = new ArrayList<>();
            while (lineScanner.hasNext()) {
                TextView textView = new TextView(context);
                textView.setText(lineScanner.next());
                textView.setTextColor(Color.BLACK);
                textView.setGravity(Gravity.CENTER_HORIZONTAL);
                textView.setBackgroundColor(Color.WHITE);
                columns.add(textView);
            }

            tableRow.setWeightSum((float) columns.size());
            for (TextView textView : columns) {
                TableRow.LayoutParams params = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                params.setMargins(2, 2, 2, 2);
                textView.setLayoutParams(params);
                tableRow.addView(textView);
            }

            rows.add(tableRow);
        }

        tableLayout.setWeightSum(rows.size());
        for (TableRow tableRow : rows) {
            TableLayout.LayoutParams params = new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1f);
            params.setMargins(2, 2, 2, 2);
            tableRow.setLayoutParams(params);
            tableLayout.addView(tableRow);
        }

        mLinearLayout.addView(titleTextView);
        mLinearLayout.addView(tableLayout);
    }

    /**
     * Formats a string by replacing new line characters with two html line breaks
     *
     * @param text string to be formatted
     * @return Spanned formatted string
     */
    private Spanned toParagraph(String text) {
        return Html.fromHtml(text);
    }

    /**
     * Copies text to the clipboard and displays a toast describing the aciton.
     *
     * @param label description of the text for the toast
     * @param text  text to be copied to the clipboard
     */
    private void copyTextToClipboard(String label, String text) {
        ClipboardManager clipBoard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(label, text);
        clipBoard.setPrimaryClip(clipData);
        Toast.makeText(getActivity(), "Copied " + label + " to the clipboard.", Toast.LENGTH_SHORT).show();
    }

    public class LoadReportPageTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            EROWID_DB.loadReport(new VaultIndexItem(mTitle, mAuthor, mSubstance, mDate, mReportUrl));
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mProgressBar.setVisibility(View.GONE);

            Context context = getContext();
            if (context == null) return;

            final String title = EROWID_DB.getReport().getTitle();
            final String author = EROWID_DB.getReport().getAuthor();
            final String substance = EROWID_DB.getReport().getSubstance();
            final String date = EROWID_DB.getReport().getDate();
            final String doseTable = EROWID_DB.getReport().getDoseTable();
            final String bodyWeight = EROWID_DB.getReport().getBodyWeight();
            final String text = EROWID_DB.getReport().getText();
            final String year = EROWID_DB.getReport().getYear();
            final String gender = EROWID_DB.getReport().getGender();
            final String id = EROWID_DB.getReport().getId();
            final String age = EROWID_DB.getReport().getAge();
            final String views = EROWID_DB.getReport().getViews();

            TextView titleTextView = new TextView(context);
            titleTextView.setText(title);
            titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            titleTextView.setTextSize(24);
            titleTextView.setTextColor(Color.BLACK);

            TextView authorTextView = new TextView(context);
            authorTextView.setText(substance);
            authorTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            authorTextView.setTextSize(20);
            authorTextView.setTextColor(Color.BLACK);

            TextView substanceTextView = new TextView(context);
            substanceTextView.setText("written by " + author);
            substanceTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            substanceTextView.setTextColor(Color.BLACK);

            TextView dateTextView = new TextView(context);
            dateTextView.setText("published on " + date);
            dateTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            dateTextView.setTextColor(Color.BLACK);

            mLinearLayout.addView(titleTextView);
            mLinearLayout.addView(authorTextView);
            mLinearLayout.addView(substanceTextView);
            mLinearLayout.addView(dateTextView);

            if (doseTable != null) addTable("Dose Table", doseTable);
            if (bodyWeight != null) addSection("Body Weight", bodyWeight);
            if (text != null) addSection("Text", toParagraph(text));
            if (year != null) addSection("Year", year);
            if (gender != null) addSection("Gender", gender);
            if (id != null) addSection("Id", id);
            if (age != null) addSection("Age", age);
            if (views != null) addSection("Views", views);

            View v = getView();
            if (v != null) v.invalidate();
        }
    }

    /**
     * Attempts to find a matching substance IndexItem and shows the menu item if available.
     */
    public class FetchMatchingSubstanceTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "FetchMatchingVaultItemTask is launched");
            mSubstanceMatch = EROWID_DB.findSubstanceMatch(new IndexItem(mName, null, mUrl, mIndexType));
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mSubstanceMatch != null) showSubstanceOption();
        }
    }
}
