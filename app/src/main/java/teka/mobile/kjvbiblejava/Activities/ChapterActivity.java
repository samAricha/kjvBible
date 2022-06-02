package teka.mobile.kjvbiblejava.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import teka.mobile.kjvbiblejava.Adapters.BibleBooksAdapter;
import teka.mobile.kjvbiblejava.Adapters.BibleVerseAdapter;
import teka.mobile.kjvbiblejava.Adapters.PagerAdapter;
import teka.mobile.kjvbiblejava.DB.Bible;
import teka.mobile.kjvbiblejava.Fragments.VerseListing;
import teka.mobile.kjvbiblejava.MISC.P;
import teka.mobile.kjvbiblejava.Models.Book;
import teka.mobile.kjvbiblejava.Models.Chapter;
import teka.mobile.kjvbiblejava.Models.Verse;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.navigation.NavigationView;
import teka.mobile.kjvbiblejava.R;


import java.util.List;

public class ChapterActivity extends AppCompatActivity implements BibleBooksAdapter.BookListener, PagerAdapter.PageListener, BibleVerseAdapter.VerseListener
        , VerseListing.ChapterInetractor {

    DrawerLayout drawerLayout;
    //recycler and adapter for the books in the quick-nav bar
    RecyclerView books_recycler;
    BibleBooksAdapter bibleBooksAdapter;
    PagerAdapter chapterAdapter;//adapter for the chapters in the books
    ViewPager viewPager;
    Bible bible;
    //spinner & adapter for the bottom chapter navigator
    ArrayAdapter<String> chaptersSpinnerAdapter;
    Spinner chapters_spinner;
    public ActionMode actionMode;
    //ActionModeCallback actionModeCallback = new ActionModeCallback();
    SharedPreferences preferences;
    private boolean forResult = false;
    View navigator;
    AppBarLayout appBar;
    ProgressDialog progressDialog;

    private int bookId, chapterId, verse_from,verse_to,totalChapters;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent = getIntent();
        bible = new Bible(this);
        forResult = intent.getBooleanExtra("forResult",false);
        bookId = intent.getIntExtra("book",1);
        // check
        chapterId = intent.getIntExtra("chapter",-1);
        verse_from = intent.getIntExtra("verse_from",0);
        verse_to = intent.getIntExtra("verse_to",0);

        viewPager = (ViewPager)findViewById(R.id.viewpager);
        initUI();
        populateChapters();
        loadBooks();

        if (forResult){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ChapterActivity.this, "Select a verse then press done", Toast.LENGTH_SHORT).show();
                }
            },1000);
        }
    }

    private void initUI() {
        progressDialog = new ProgressDialog(this);
        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView)findViewById(R.id.navigation_view);
        View body = getLayoutInflater().inflate(R.layout.bible_side_nav,navigationView,false);
        navigationView.addHeaderView(body);
        books_recycler = (RecyclerView)body.findViewById(R.id.recycler);
        books_recycler.setHasFixedSize(false);
        bibleBooksAdapter = new BibleBooksAdapter(this,R.layout.row_book_small);
        books_recycler.setAdapter(bibleBooksAdapter);
        books_recycler.setLayoutManager(new LinearLayoutManager(this));

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(bible.bookName(bookId));//setting the title to book name
        getSupportActionBar().setSubtitle("Chapter 1");//setting initial chapter to 1
        //toolbar.setNavigationIcon(new IconicsDrawable(this, FontAwesome.Icon.faw_bars).color(Color.WHITE).sizeDp(20));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        //assigning the viewpager to an adapter(PagerAdapter)
        chapterAdapter = new PagerAdapter(getSupportFragmentManager(),this);
        viewPager = (ViewPager)findViewById(R.id.viewpager);
        viewPager.setAdapter(chapterAdapter);
        //viewPager.setPageTransformer(true, (ViewPager.PageTransformer) new StackTransformer());
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                getSupportActionBar().setTitle(bible.bookName(bookId));
                getSupportActionBar().setSubtitle("Chapter "+chapterAdapter.getPageTitle(position));
                chapters_spinner.setSelection(position);
                if (actionMode != null){
                    actionMode.finish();
                    try {
                        ((VerseListing)chapterAdapter.getItem(position-1)).clearSelection();
                    }catch (Exception e){}
                    try {
                        ((VerseListing)chapterAdapter.getItem(position+1)).clearSelection();
                    }catch (Exception e){}
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        //Navigators at the bottom of the page
        navigator = findViewById(R.id.chapter_navigator);
        View move_left = navigator.findViewById(R.id.move_left);
        View move_right = navigator.findViewById(R.id.move_right);
        chapters_spinner = (Spinner)navigator.findViewById(R.id.chapters);
        chaptersSpinnerAdapter = new ArrayAdapter<String>(this,R.layout.chapter_spinner_item);
        chapters_spinner.setAdapter(chaptersSpinnerAdapter);
        chapters_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                viewPager.setCurrentItem(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        move_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() > 0 ? viewPager.getCurrentItem() - 1 : 0);
            }
        });
        move_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() < chapterAdapter.getCount() - 1 ? viewPager.getCurrentItem() + 1 : chapterAdapter.getCount() - 1);
            }
        });
        move_right.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                viewPager.setCurrentItem(chapterAdapter.getCount() - 1);
                return true;
            }
        });
        move_left.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                viewPager.setCurrentItem(0);
                return true;
            }
        });

        appBar = (AppBarLayout)findViewById(R.id.appbar);
        appBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                navigator.setTranslationY(-(verticalOffset * 2));
            }
        });


        progressDialog.setCancelable(false);
        progressDialog.setMessage("Setting up...");

    }


    //   POPULATING THE CHAPTERS IN THE RECYCLER VIEW
    private void populateChapters() {
        chapterAdapter.clearAll();
        chaptersSpinnerAdapter.clear();
        chaptersSpinnerAdapter.notifyDataSetChanged();
        Toast.makeText(ChapterActivity.this, bible.bookName(bookId) + bible.bookChapters(bookId)+1, Toast.LENGTH_LONG).show();
        chapterAdapter = new PagerAdapter(getSupportFragmentManager(),this);
        viewPager.setAdapter(chapterAdapter);
        // Chapter +1 (includes the last chapter)
        totalChapters = bible.bookChapters(bookId)+1;
        Log.e("CHAPTER DISCOVERED",totalChapters+"");
        Log.e("CHAPTER DISCOVERED", bookId + "," + chapterId + "," + verse_from + "," + verse_to);
        if (chapterId > 0){
            getSupportActionBar().setTitle(bible.bookName(bookId));
            //getSupportActionBar().setSubtitle("Chapter " + chapter);//changed from to
            getSupportActionBar().setSubtitle("Chapter 1");
            if (chapterId>1){
                VerseListing verseListing = new VerseListing();
                verseListing.setArguments(P.chapterToBundle(new Chapter(bookId, chapterId - 1)));
                chapterAdapter.addFrag(verseListing, (chapterId-1)+"");
            }
            VerseListing verseListing = new VerseListing();
            Bundle bundle = P.chapterToBundle(new Chapter(bookId, chapterId));
            bundle.putInt("from",verse_from);
            bundle.putInt("to",verse_to);
            verseListing.setArguments(bundle);
            chapterAdapter.addFrag(verseListing, (chapterId) + "");
            VerseListing verseListing2 = new VerseListing();
            Bundle bundle2 = P.chapterToBundle(new Chapter(bookId, chapterId + 1));
            verseListing2.setArguments(bundle2);
            chapterAdapter.addFrag(verseListing2, (chapterId + 1) + "");
            chapterAdapter.notifyDataSetChanged();
            viewPager.setCurrentItem(chapterAdapter.getCount() == 3 ? 1 : 0);

            return;
        }



        for (int a = 1; a < totalChapters; a++) {
            VerseListing verseListing = new VerseListing();
            verseListing.setArguments(P.chapterToBundle(new Chapter(bookId, a)));
            chapterAdapter.addFrag(verseListing, (a) + "");
        }
        chapterAdapter.notifyDataSetChanged();

        populateChapterSpinner();
    }

    private void populateChapterSpinner() {
        chaptersSpinnerAdapter.clear();
        chapters_spinner.setSelection(0);
        //+1 chapter (adds the last Chapter)
        if (totalChapters<1)chaptersSpinnerAdapter.add("Chapter");
        for (int a = 1; a < totalChapters; a++) {
            chaptersSpinnerAdapter.add("Ch " + a);
        }
    }


    public void loadBooks(){
//        if (progressDialog.isShowing())progressDialog.dismiss();
//        progressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i<67;i++){
                    String name = bible.bookName(i);
                    Book book = new Book(i,0,name);
                    bibleBooksAdapter.addBook(book);
                }
                mainHandler.sendEmptyMessage(0);
            }
        }).start();
    }

    Handler mainHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            bibleBooksAdapter.notifyDataSetChanged();
//            progressDialog.dismiss();
            drawerLayout.scrollTo(0,0);
        }
    };


    @Override
    public void bookClicked(Book book) {
        this.bookId = book.getIndex();
        getSupportActionBar().setTitle(bible.bookName(this.bookId));
        getSupportActionBar().setSubtitle("Chapter 1");
        chapterId = -1; verse_from = 0; verse_to = 0;
        populateChapters();
        drawerLayout.closeDrawers();
    }

    @Override
    public void verseClicked(Verse verse) {

    }

    @Override
    public void verseLongClicked(Verse verse) {

    }

    @Override
    public void pageCount(int size) {

    }

    @Override
    public void onVerseSelectionChange(List<Verse> selection) {

    }
}