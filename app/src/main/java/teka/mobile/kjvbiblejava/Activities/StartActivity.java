package teka.mobile.kjvbiblejava.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import teka.mobile.kjvbiblejava.Adapters.BibleBooksAdapter;
import teka.mobile.kjvbiblejava.DB.Bible;
import teka.mobile.kjvbiblejava.DB.DBHelper;
import teka.mobile.kjvbiblejava.DB.paths;
import teka.mobile.kjvbiblejava.Models.Book;
import teka.mobile.kjvbiblejava.Models.Chapter;
import teka.mobile.kjvbiblejava.R;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StartActivity extends AppCompatActivity implements BibleBooksAdapter.BookListener{

    View quick_nav,go,cover;
    ProgressBar progressBar;
    RecyclerView recycler;
    Bible bible;
    BibleBooksAdapter bibleBooksAdapter;
    ArrayAdapter<String> booksAdapter,chaptersAdapter,verseAdapter;
    Spinner booksSpinner,chaptersSpinner,versesSpinner;
    DBHelper dbhelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        bible = new Bible(this);
        prepareDb();
        initUI();
    }

    //Accessing and opening the database
    private void prepareDb() {
        dbhelper = new DBHelper(getApplicationContext());
        //we first of all create the database if not yet created.
        try {
            dbhelper.createDatabase();

        } catch (IOException e) {
            e.printStackTrace();
            throw new Error(e.getMessage());

        }

        //we then open the db.
        try {
            dbhelper.openDatabase();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throw new Error(throwables.getMessage());

        }
    }

    //INITIALIZING THE UI i.e the BIBLE COVER FOLLOWED BY THE BIBLE BOOKS
    private void initUI() {
        Toast.makeText(getApplicationContext(), "welcome", Toast.LENGTH_LONG).show();
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("The Holy Bible");

        progressBar = findViewById(R.id.progressBar);
        quick_nav = findViewById(R.id.quick_nav);

        booksSpinner = (Spinner)findViewById(R.id.book);
        chaptersSpinner = (Spinner)findViewById(R.id.chapter);
        versesSpinner = (Spinner)findViewById(R.id.verse);

        //Toast.makeText(getApplicationContext(), )
        if (paths.bibleAvailable()){
            populateRecycler();
            quick_nav.setVisibility(View.VISIBLE);
        }else {
            quick_nav.setVisibility(View.GONE);
        }

        go = findViewById(R.id.go);
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selectedItemOfMySpinner = booksSpinner.getSelectedItemPosition();
                String actualPositionOfMySpinner = (String) booksSpinner.getItemAtPosition(selectedItemOfMySpinner);
                if (actualPositionOfMySpinner.equals("Book")) {
                    Toast.makeText(getApplicationContext(), "CHOOSE BOOK", Toast.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(getBaseContext(),ChapterActivity.class);
                intent.putExtra("book",booksSpinner.getSelectedItemPosition());
                intent.putExtra("chapter",chaptersSpinner.getSelectedItemPosition()+1);
                intent.putExtra("verse_from",versesSpinner.getSelectedItemPosition());
                startActivity(intent);
            }
        });


        //DISPLAYING THE BIBLE COVER BEFORE DISPLAYING THE BIBLE BOOKS
        cover = findViewById(R.id.cover);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                cover.animate().translationX(-700).setDuration(500).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {}

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        cover.setVisibility(View.GONE);
                    }
                    @Override
                    public void onAnimationCancel(Animator animator) {}
                    @Override
                    public void onAnimationRepeat(Animator animator) {}
                });
            }
        },2000);


    }

    //populates the recycler view with books from the bible
    private void populateRecycler() {
        recycler = (RecyclerView) findViewById(R.id.recycler);
        recycler.setHasFixedSize(true);
        bibleBooksAdapter = new BibleBooksAdapter(this,R.layout.row_book);
        recycler.setAdapter(bibleBooksAdapter);
        recycler.setLayoutManager(new GridLayoutManager(this, 2));

        //setting up the book spinner
        booksAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line);
        booksAdapter.add("Book");
        booksSpinner.setAdapter(booksAdapter);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] books = new String[67];
                for (int i = 1; i<67;i++){
                    String name = bible.bookName(i);
                    Book book = new Book(i,0,name);
                    books[i-1] = name;
                    bibleBooksAdapter.addBook(book);
                    mainHandler.sendEmptyMessage(0);
                }
                Message message = new Message();
                message.getData().putStringArray("books", books);
                mainHandler.sendMessage(message);
            }
        }).start();

        //setting up the chapter spinner
        final ArrayAdapter<String> chaptersAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line);
        chaptersAdapter.add("Chapter");
        chaptersSpinner.setAdapter(chaptersAdapter);

        //setting up the verse spinner
        final ArrayAdapter<String> versesAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line);
        versesAdapter.add("Verse");
        versesSpinner.setAdapter(versesAdapter);

        booksSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                chaptersAdapter.clear();
                //+1 chapter (adds the last Chapter)
                int chapters = bible.bookChapters(i);
                if (chapters<1)chaptersAdapter.add("Chapter");
                for (int a = 1; a < chapters; a++) {
                    chaptersAdapter.add("Ch " + a);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        chaptersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int a, long l) {
                List verses = bible.getVerses(new Chapter(booksSpinner.getSelectedItemPosition(),a+1));
                versesAdapter.clear();
                if (verses.isEmpty())versesAdapter.add("Verse");
                for (int i = 1; i<verses.size();i++){
                    versesAdapter.add("Vs "+i);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

    }

    Handler mainHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            String[]books = msg.getData().getStringArray("books");
            if (books == null){
                bibleBooksAdapter.notifyDataSetChanged();
            }else {
                booksAdapter.addAll(books);
            }
        }
    };

    @Override
    public void bookClicked(Book book) {
        Intent intent = new Intent(this, ChapterActivity.class);
        intent.putExtra("book",book.getIndex());
        startActivity(intent);
    }



    private int backPresses = 0;
    @Override
    public void onBackPressed() {
        backPresses++;
        if (backPresses <2){
            Toast.makeText(StartActivity.this, "Double press to exit", Toast.LENGTH_LONG).show();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bible, menu);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
        searchView.setOnQueryTextListener(queryTextListener);
        searchView.setGravity(Gravity.TOP);
        searchView.setQueryHint("Search book..");
        return true;
    }


    SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            List<Book> filterList = filter(bibleBooksAdapter.getBooks(), newText);
            if(newText != null || newText != ""){
                if (newText.length()>0){
                    bibleBooksAdapter.animateTo(filterList);
                    recycler.scrollToPosition(0);
                }else {
                    bibleBooksAdapter.reload();
                }
            }
            return true;
        }
    };


    private List<Book> filter(List<Book> list, String query){
        query = query.toLowerCase();
        final List<Book> filterlist = new ArrayList<>();
        for(Book book: list){
            String text = book.getName().toLowerCase();
            if(text.contains(query) && !filterlist.contains(book)){
                filterlist.add(book);
            }
        }
        return filterlist;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}