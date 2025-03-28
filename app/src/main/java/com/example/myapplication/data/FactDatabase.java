package com.example.myapplication.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FactDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "facts.db";
    private static final int DATABASE_VERSION = 1;

    // Table name
    private static final String TABLE_FACTS = "facts";

    // Column names
    private static final String KEY_ID = "id";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_IS_TRUE = "is_true";
    private static final String KEY_IS_SHOWN = "is_shown";

    private static FactDatabase instance;

    // Singleton pattern
    public static synchronized FactDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new FactDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private FactDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_FACTS_TABLE = "CREATE TABLE " + TABLE_FACTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_CONTENT + " TEXT,"
                + KEY_CATEGORY + " TEXT,"
                + KEY_SOURCE + " TEXT,"
                + KEY_IS_TRUE + " INTEGER,"
                + KEY_IS_SHOWN + " INTEGER DEFAULT 0"
                + ")";
        db.execSQL(CREATE_FACTS_TABLE);

        // Populate with initial facts
        populateInitialFacts(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FACTS);
        onCreate(db);
    }

    // Add a new fact
    public long addFact(Fact fact) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CONTENT, fact.getContent());
        values.put(KEY_CATEGORY, fact.getCategory().toString());
        values.put(KEY_SOURCE, fact.getSource());
        values.put(KEY_IS_TRUE, fact.isTrue() ? 1 : 0);
        values.put(KEY_IS_SHOWN, fact.isShown() ? 1 : 0);

        // Insert row
        long id = db.insert(TABLE_FACTS, null, values);
        db.close();
        return id;
    }

    // Get a random fact
    public Fact getRandomFact(Fact.Category category) {
        String categoryFilter = "";
        if (category != null && category != Fact.Category.ALL) {
            categoryFilter = " WHERE " + KEY_CATEGORY + " = '" + category.toString() + "'";
        }
        String query = "SELECT * FROM " + TABLE_FACTS + categoryFilter + " ORDER BY RANDOM() LIMIT 1";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        Fact fact = null;
        if (cursor.moveToFirst()) {
            fact = cursorToFact(cursor);
        }
        cursor.close();
        db.close();
        return fact;
    }

    // Get a random unshown fact
    public Fact getRandomUnshownFact(Fact.Category category) {
        String categoryFilter = "";
        if (category != null && category != Fact.Category.ALL) {
            categoryFilter = " AND " + KEY_CATEGORY + " = '" + category.toString() + "'";
        }
        String query = "SELECT * FROM " + TABLE_FACTS + " WHERE " + KEY_IS_SHOWN + " = 0" 
                + categoryFilter + " ORDER BY RANDOM() LIMIT 1";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        Fact fact = null;
        if (cursor.moveToFirst()) {
            fact = cursorToFact(cursor);
        } else {
            // If no unshown facts, reset all facts and try again
            resetShownStatus();
            cursor.close();
            db.close();
            return getRandomUnshownFact(category);
        }
        cursor.close();
        db.close();
        return fact;
    }

    // Mark a fact as shown
    public void markFactAsShown(int factId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_IS_SHOWN, 1);

        db.update(TABLE_FACTS, values, KEY_ID + " = ?", new String[]{String.valueOf(factId)});
        db.close();
    }

    // Reset all facts to unshown
    public void resetShownStatus() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_IS_SHOWN, 0);

        db.update(TABLE_FACTS, values, null, null);
        db.close();
    }

    // Get quiz facts (random mix of true and false)
    public List<Fact> getQuizFacts(int count) {
        List<Fact> facts = new ArrayList<>();
        
        String queryTrue = "SELECT * FROM " + TABLE_FACTS + " WHERE " + KEY_IS_TRUE + " = 1 ORDER BY RANDOM() LIMIT " + (count / 2);
        String queryFalse = "SELECT * FROM " + TABLE_FACTS + " WHERE " + KEY_IS_TRUE + " = 0 ORDER BY RANDOM() LIMIT " + (count - count / 2);

        SQLiteDatabase db = this.getReadableDatabase();
        
        // Get true facts
        Cursor cursor = db.rawQuery(queryTrue, null);
        if (cursor.moveToFirst()) {
            do {
                facts.add(cursorToFact(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        // Get false facts
        cursor = db.rawQuery(queryFalse, null);
        if (cursor.moveToFirst()) {
            do {
                facts.add(cursorToFact(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        // Shuffle the list
        shuffleList(facts);
        
        db.close();
        return facts;
    }

    // Helper method to convert cursor to Fact
    private Fact cursorToFact(Cursor cursor) {
        Fact fact = new Fact();
        fact.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
        fact.setContent(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTENT)));
        fact.setCategory(Fact.Category.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY))));
        fact.setSource(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SOURCE)));
        fact.setTrue(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_TRUE)) == 1);
        fact.setShown(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_SHOWN)) == 1);
        return fact;
    }

    // Shuffle a list using Fisher-Yates algorithm
    private void shuffleList(List<Fact> list) {
        Random rnd = new Random();
        for (int i = list.size() - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            Fact temp = list.get(index);
            list.set(index, list.get(i));
            list.set(i, temp);
        }
    }

    // Get total count of facts in database
    public int getFactCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_FACTS;
        Cursor cursor = db.rawQuery(query, null);
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    // Populate with initial facts
    private void populateInitialFacts(SQLiteDatabase db) {
        // Science facts
        addFactToDB(db, "The average human body contains enough fat to make 7 bars of soap.", Fact.Category.SCIENCE, "National Geographic", true);
        addFactToDB(db, "Diamonds can be made from peanut butter.", Fact.Category.SCIENCE, "Scientific American", true);
        addFactToDB(db, "Bananas are radioactive due to their potassium content.", Fact.Category.SCIENCE, "World Nuclear Association", true);
        addFactToDB(db, "A day on Venus is longer than a year on Venus.", Fact.Category.SCIENCE, "NASA", true);
        addFactToDB(db, "Human stomachs can dissolve razor blades.", Fact.Category.SCIENCE, "Scientific American", true);
        addFactToDB(db, "The human nose can detect over 1 trillion different scents.", Fact.Category.SCIENCE, "Science Journal", true);
        addFactToDB(db, "About 8% of human DNA comes from viruses that infected our ancestors.", Fact.Category.SCIENCE, "Scientific American", true);
        addFactToDB(db, "The average person walks the equivalent of three times around the world in a lifetime.", Fact.Category.SCIENCE, "World Health Organization", true);
        addFactToDB(db, "Brain information travels up to 268 miles per hour.", Fact.Category.SCIENCE, "Journal of Neuroscience", true);
        addFactToDB(db, "Every second, the Sun converts 600 million tons of hydrogen into helium.", Fact.Category.SCIENCE, "NASA", true);
        
        // History facts
        addFactToDB(db, "Napoleon Bonaparte was actually of average height for his time.", Fact.Category.HISTORY, "Smithsonian Magazine", true);
        addFactToDB(db, "Vikings never actually wore horned helmets.", Fact.Category.HISTORY, "History Channel", true);
        addFactToDB(db, "The Great Wall of China is not visible from space with the naked eye.", Fact.Category.HISTORY, "NASA", true);
        addFactToDB(db, "Cleopatra lived closer in time to the invention of the iPhone than to the construction of the Great Pyramid.", Fact.Category.HISTORY, "BBC History", true);
        addFactToDB(db, "Oxford University is older than the Aztec Empire.", Fact.Category.HISTORY, "Oxford University", true);
        addFactToDB(db, "Ancient Romans used urine as mouthwash.", Fact.Category.HISTORY, "Smithsonian Magazine", true);
        addFactToDB(db, "The shortest war in history was between Britain and Zanzibar on August 27, 1896. It lasted only 38 minutes.", Fact.Category.HISTORY, "Royal Museums Greenwich", true);
        addFactToDB(db, "Ninjas wore dark blue, not black, for better night camouflage.", Fact.Category.HISTORY, "Japanese Historical Society", true);
        addFactToDB(db, "The first Olympics included a running event where participants ran in full armor.", Fact.Category.HISTORY, "Olympic Historical Society", true);
        addFactToDB(db, "The first documented case of a man giving up his seat on a lifeboat for a woman was on the Titanic.", Fact.Category.HISTORY, "British Maritime Museum", true);
        
        // Animal facts
        addFactToDB(db, "Octopuses have three hearts.", Fact.Category.ANIMALS, "National Geographic", true);
        addFactToDB(db, "Flamingos turn pink because of the food they eat.", Fact.Category.ANIMALS, "Audubon Society", true);
        addFactToDB(db, "A cockroach can live for a week without its head.", Fact.Category.ANIMALS, "National Geographic", true);
        addFactToDB(db, "Honey never spoils. Archaeologists have found pots of honey in ancient Egyptian tombs that are over 3,000 years old and still perfectly good to eat.", Fact.Category.ANIMALS, "Smithsonian", true);
        addFactToDB(db, "Koalas have fingerprints so similar to humans that they've occasionally confounded crime scene investigators.", Fact.Category.ANIMALS, "National Geographic", true);
        addFactToDB(db, "Cows can sleep standing up but can only dream lying down.", Fact.Category.ANIMALS, "American Museum of Natural History", true);
        addFactToDB(db, "Rabbits can see behind themselves without turning their heads.", Fact.Category.ANIMALS, "National Geographic", true);
        addFactToDB(db, "A group of flamingos is called a 'flamboyance'.", Fact.Category.ANIMALS, "Cornell University Ornithology Lab", true);
        addFactToDB(db, "Hummingbirds are the only birds that can fly backwards.", Fact.Category.ANIMALS, "Audubon Society", true);
        addFactToDB(db, "Sharks have existed for more than 450 million years, longer than trees and dinosaurs.", Fact.Category.ANIMALS, "National Oceanic and Atmospheric Administration", true);
        
        // Geography facts
        addFactToDB(db, "Alaska is the easternmost and westernmost state in the United States.", Fact.Category.GEOGRAPHY, "National Geographic", true);
        addFactToDB(db, "Russia has 11 time zones.", Fact.Category.GEOGRAPHY, "World Atlas", true);
        addFactToDB(db, "The Sahara Desert was once a lush green landscape with lakes and rivers.", Fact.Category.GEOGRAPHY, "National Geographic", true);
        addFactToDB(db, "Australia is wider than the moon.", Fact.Category.GEOGRAPHY, "NASA", true);
        addFactToDB(db, "There is a waterfall underwater in the Denmark Strait.", Fact.Category.GEOGRAPHY, "NOAA", true);
        addFactToDB(db, "The tallest mountain on Earth is actually Mauna Kea in Hawaii, not Everest, if measured from its base under the ocean.", Fact.Category.GEOGRAPHY, "USGS", true);
        addFactToDB(db, "Point Nemo in the Pacific Ocean is so remote that the nearest humans are often astronauts on the ISS.", Fact.Category.GEOGRAPHY, "NOAA", true);
        addFactToDB(db, "Vatican City is the smallest country in the world, with an area of just 0.17 square miles.", Fact.Category.GEOGRAPHY, "United Nations", true);
        addFactToDB(db, "The Amazon River flows backwards for part of the year due to tidal effects.", Fact.Category.GEOGRAPHY, "Scientific American", true);
        addFactToDB(db, "The largest desert in the world is Antarctica, not the Sahara.", Fact.Category.GEOGRAPHY, "National Geographic", true);
        
        // Physics facts
        addFactToDB(db, "Time passes faster at your face than at your feet.", Fact.Category.PHYSICS, "Scientific American", true);
        addFactToDB(db, "If you could fold a piece of paper 42 times, it would reach the moon.", Fact.Category.PHYSICS, "NASA", true);
        addFactToDB(db, "Neutron stars are so dense that a teaspoon would weigh about 10 million tons.", Fact.Category.PHYSICS, "Space.com", true);
        addFactToDB(db, "The weight of light from a full moon on a lake is about 1 pound spread across the entire surface.", Fact.Category.PHYSICS, "American Physical Society", true);
        addFactToDB(db, "If you shout for 8 years, 7 months and 6 days, you will produce enough energy to heat a cup of coffee.", Fact.Category.PHYSICS, "Physics Journal", true);
        addFactToDB(db, "Quantum entanglement allows particles to influence each other instantaneously, regardless of distance.", Fact.Category.PHYSICS, "MIT Technology Review", true);
        addFactToDB(db, "Light behaves both as a particle and as a wave.", Fact.Category.PHYSICS, "American Institute of Physics", true);
        addFactToDB(db, "Absolute zero is −273.15°C (−459.67°F), the lowest temperature theoretically possible.", Fact.Category.PHYSICS, "American Physical Society", true);
        addFactToDB(db, "It would take 1.2 million mosquitoes, each sucking once, to completely drain the average human of blood.", Fact.Category.PHYSICS, "Journal of Applied Physics", true);
        addFactToDB(db, "There are more atoms in a glass of water than there are glasses of water in all the oceans on Earth.", Fact.Category.PHYSICS, "American Institute of Physics", true);
        
        // Astronomy facts
        addFactToDB(db, "One million Earths could fit inside the Sun.", Fact.Category.ASTRONOMY, "NASA", true);
        addFactToDB(db, "A year on Venus is shorter than a day on Venus.", Fact.Category.ASTRONOMY, "NASA", true);
        addFactToDB(db, "There is a planet made of diamonds, called 55 Cancri e.", Fact.Category.ASTRONOMY, "Yale University", true);
        addFactToDB(db, "Saturn's rings are disappearing and will be gone in about 100 million years.", Fact.Category.ASTRONOMY, "NASA", true);
        addFactToDB(db, "The largest known star, UY Scuti, is more than 1,700 times larger than our Sun.", Fact.Category.ASTRONOMY, "European Southern Observatory", true);
        addFactToDB(db, "There are more stars in the universe than grains of sand on all the beaches on Earth.", Fact.Category.ASTRONOMY, "NASA", true);
        addFactToDB(db, "The footprints left by Apollo astronauts on the Moon will likely last for at least 100 million years.", Fact.Category.ASTRONOMY, "NASA", true);
        addFactToDB(db, "A day on Mercury lasts approximately two Mercury years.", Fact.Category.ASTRONOMY, "NASA", true);
        addFactToDB(db, "The core of Jupiter is hotter than the surface of the Sun.", Fact.Category.ASTRONOMY, "Space.com", true);
        addFactToDB(db, "The Milky Way galaxy is on a collision course with the Andromeda galaxy, expected to occur in about 4.5 billion years.", Fact.Category.ASTRONOMY, "Hubble Space Telescope", true);
        
        // Economics facts
        addFactToDB(db, "Zimbabwe once had a 100 trillion dollar bill due to hyperinflation.", Fact.Category.ECONOMICS, "IMF", true);
        addFactToDB(db, "The most expensive pizza in the world costs $12,000.", Fact.Category.ECONOMICS, "Guinness World Records", true);
        addFactToDB(db, "The world's first stock exchange was established in Amsterdam in 1602.", Fact.Category.ECONOMICS, "The Economist", true);
        addFactToDB(db, "During the 2008 hyperinflation in Zimbabwe, prices doubled every 24 hours.", Fact.Category.ECONOMICS, "World Bank", true);
        addFactToDB(db, "US $100 bills have an average lifespan of 15 years, the longest of any denomination.", Fact.Category.ECONOMICS, "Federal Reserve", true);
        addFactToDB(db, "Less than 8% of the world's currency exists as physical money; the rest is digital.", Fact.Category.ECONOMICS, "Bank for International Settlements", true);
        addFactToDB(db, "The term 'salary' comes from the Latin word 'salarium,' which referred to the money given to Roman soldiers to buy salt.", Fact.Category.ECONOMICS, "Oxford Dictionary", true);
        addFactToDB(db, "The most expensive domain name ever sold was Cars.com for $872 million.", Fact.Category.ECONOMICS, "Business Insider", true);
        addFactToDB(db, "In 1923, Germany experienced such severe hyperinflation that people needed wheelbarrows of money to buy bread.", Fact.Category.ECONOMICS, "Bundesbank", true);
        addFactToDB(db, "The New York Stock Exchange was founded in 1792 under a buttonwood tree on Wall Street.", Fact.Category.ECONOMICS, "NYSE", true);
        
        // Linguistics facts
        addFactToDB(db, "The longest word in English is 'pneumonoultramicroscopicsilicovolcanoconiosis'.", Fact.Category.LINGUISTICS, "Oxford Dictionary", true);
        addFactToDB(db, "The word 'set' has the most definitions in the English language.", Fact.Category.LINGUISTICS, "Oxford Dictionary", true);
        addFactToDB(db, "There are over 200 artificial languages that have been created for books, TV shows, and movies.", Fact.Category.LINGUISTICS, "Linguistic Society of America", true);
        addFactToDB(db, "The most commonly used letter in English is 'E'.", Fact.Category.LINGUISTICS, "Cornell University", true);
        addFactToDB(db, "'I am' is the shortest complete sentence in English.", Fact.Category.LINGUISTICS, "Cambridge University Press", true);
        addFactToDB(db, "The dot over the letter 'i' is called a tittle.", Fact.Category.LINGUISTICS, "Merriam-Webster", true);
        addFactToDB(db, "The only word in English that ends with the letters 'mt' is 'dreamt'.", Fact.Category.LINGUISTICS, "Linguistic Society of America", true);
        addFactToDB(db, "The word 'queue' is the only English word that is pronounced the same when the last four letters are removed.", Fact.Category.LINGUISTICS, "Journal of Linguistics", true);
        addFactToDB(db, "There are languages that have no written form and exist only as spoken languages.", Fact.Category.LINGUISTICS, "UNESCO", true);
        addFactToDB(db, "The word 'almost' is the longest in the English language with all letters in alphabetical order.", Fact.Category.LINGUISTICS, "Linguistic Society of America", true);
        
        // Music facts
        addFactToDB(db, "The world's longest concert lasted for 639 hours.", Fact.Category.MUSIC, "Guinness World Records", true);
        addFactToDB(db, "Mozart composed his first piece of music at age 5.", Fact.Category.MUSIC, "Classical Archives", true);
        addFactToDB(db, "The Beatles couldn't read music.", Fact.Category.MUSIC, "Rolling Stone", true);
        addFactToDB(db, "The most expensive musical instrument sold was the 'Vieuxtemps' Guarneri violin for $16 million.", Fact.Category.MUSIC, "Christie's", true);
        addFactToDB(db, "The longest commercially released song is 'The Rise and Fall of Bossanova' by PC III, lasting 13 hours, 23 minutes, and 32 seconds.", Fact.Category.MUSIC, "Guinness World Records", true);
        addFactToDB(db, "Michael Jackson's 'Thriller' album is the best-selling album of all time.", Fact.Category.MUSIC, "Recording Industry Association of America", true);
        addFactToDB(db, "The original name of the band Queen was 'Smile'.", Fact.Category.MUSIC, "Queen Archives", true);
        addFactToDB(db, "The first music video broadcast on MTV was 'Video Killed the Radio Star' by The Buggles.", Fact.Category.MUSIC, "MTV", true);
        addFactToDB(db, "Prince played 27 instruments on his first album.", Fact.Category.MUSIC, "Warner Bros Records", true);
        addFactToDB(db, "Ludwig van Beethoven composed some of his greatest works after becoming completely deaf.", Fact.Category.MUSIC, "Beethoven-Haus Bonn", true);
        
        // Literature facts
        addFactToDB(db, "The longest novel ever published is 'Artamène ou le Grand Cyrus' with over 2 million words.", Fact.Category.LITERATURE, "Guinness World Records", true);
        addFactToDB(db, "Shakespeare invented over 1,700 words that we still use today.", Fact.Category.LITERATURE, "Shakespeare Birthplace Trust", true);
        addFactToDB(db, "J.K. Rowling was rejected by 12 publishers before Bloomsbury accepted Harry Potter.", Fact.Category.LITERATURE, "Bloomsbury Publishing", true);
        addFactToDB(db, "Agatha Christie wrote 66 detective novels during her lifetime.", Fact.Category.LITERATURE, "Agatha Christie Ltd", true);
        addFactToDB(db, "The word 'robot' was introduced to the English language by Czech writer Karel Čapek in his 1920 play 'R.U.R.'", Fact.Category.LITERATURE, "Oxford English Dictionary", true);
        addFactToDB(db, "The original manuscript of 'Alice in Wonderland' was titled 'Alice's Adventures Under Ground'.", Fact.Category.LITERATURE, "British Library", true);
        addFactToDB(db, "Ernest Hemingway wrote standing up.", Fact.Category.LITERATURE, "Ernest Hemingway Foundation", true);
        addFactToDB(db, "The 'Oxford English Dictionary' took 70 years to complete.", Fact.Category.LITERATURE, "Oxford University Press", true);
        addFactToDB(db, "Dr. Seuss wrote 'Green Eggs and Ham' on a bet that he couldn't write a book using only 50 different words.", Fact.Category.LITERATURE, "Random House", true);
        addFactToDB(db, "Mary Shelley wrote 'Frankenstein' when she was just 18 years old.", Fact.Category.LITERATURE, "British Library", true);
    }

    // Helper method to add fact directly to database
    private void addFactToDB(SQLiteDatabase db, String content, Fact.Category category, String source, boolean isTrue) {
        ContentValues values = new ContentValues();
        values.put(KEY_CONTENT, content);
        values.put(KEY_CATEGORY, category.toString());
        values.put(KEY_SOURCE, source);
        values.put(KEY_IS_TRUE, isTrue ? 1 : 0);
        values.put(KEY_IS_SHOWN, 0);
        db.insert(TABLE_FACTS, null, values);
    }
} 