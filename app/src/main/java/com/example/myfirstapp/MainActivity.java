package com.example.myfirstapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private TextView questionNumberText;
    private TextView scoreText;
    private TextView questionText;
    private Button[] answerButtons;
    private Button hint5050Button;
    private Button hintAudienceButton;
    private Button hintPhoneButton;

    private int currentQuestion = 1;
    private int currentScore = 0;
    private int correctAnswerIndex = 1; // By default, second answer (index 1) is correct
    
    // Real prize values from the show
    private final int[] prizeValues = {
        100, 200, 300, 500, 1000,
        2000, 4000, 8000, 16000, 32000,
        64000, 125000, 250000, 500000, 1000000
    };
    
    private boolean[] hintsUsed = new boolean[3]; // 50:50, Audience, Phone
    private List<Integer> usedQuestionIndices = new ArrayList<>();
    private Random random = new Random();
    private int totalQuestions = 15; // 15 questions to win the game
    private int availableQuestions = 100; // We have 100 questions in our bank
    private int currentQuestionIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        questionNumberText = findViewById(R.id.questionNumberText);
        scoreText = findViewById(R.id.scoreText);
        questionText = findViewById(R.id.questionText);
        
        // Initialize answer buttons
        answerButtons = new Button[4];
        answerButtons[0] = findViewById(R.id.answerButton1);
        answerButtons[1] = findViewById(R.id.answerButton2);
        answerButtons[2] = findViewById(R.id.answerButton3);
        answerButtons[3] = findViewById(R.id.answerButton4);

        // Initialize hint buttons
        hint5050Button = findViewById(R.id.hint5050Button);
        hintAudienceButton = findViewById(R.id.hintAudienceButton);
        hintPhoneButton = findViewById(R.id.hintPhoneButton);

        // Set click listeners for hint buttons
        hint5050Button.setOnClickListener(v -> useHint5050());
        hintAudienceButton.setOnClickListener(v -> useHintAudience());
        hintPhoneButton.setOnClickListener(v -> useHintPhone());

        // Set click listeners for answer buttons
        for (int i = 0; i < answerButtons.length; i++) {
            final int answerIndex = i;
            answerButtons[i].setOnClickListener(v -> checkAnswer(answerIndex));
        }

        // Start the game
        resetGame();
    }

    private void updateQuestion() {
        // Update question number and score
        questionNumberText.setText(getString(R.string.question_text, currentQuestion));
        scoreText.setText(getString(R.string.score_text, currentScore));

        // Select a new random question index that hasn't been used
        do {
            currentQuestionIndex = random.nextInt(availableQuestions) + 1;
        } while (usedQuestionIndices.contains(currentQuestionIndex));
        
        usedQuestionIndices.add(currentQuestionIndex);
        
        // Set question text based on random question
        String questionResourceName = "q" + currentQuestionIndex;
        int questionResId = getResources().getIdentifier(questionResourceName, "string", getPackageName());
        
        if (questionResId != 0) {
            questionText.setText(questionResId);
            
            // Set answer texts
            for (int i = 0; i < answerButtons.length; i++) {
                String answerResourceName = "q" + currentQuestionIndex + "_a" + (i + 1);
                int answerResId = getResources().getIdentifier(answerResourceName, "string", getPackageName());
                
                if (answerResId != 0) {
                    answerButtons[i].setText(answerResId);
                    answerButtons[i].setVisibility(View.VISIBLE);
                } else {
                    Log.e("MainActivity", "Answer resource not found: " + answerResourceName);
                }
            }
            
            // For this game, all questions use the same pattern: second answer (index 1) is correct
            correctAnswerIndex = 1;
        } else {
            Log.e("MainActivity", "Question resource not found: " + questionResourceName);
        }
    }

    private void checkAnswer(int selectedAnswer) {
        boolean isCorrect = selectedAnswer == correctAnswerIndex;

        if (isCorrect) {
            currentScore = prizeValues[currentQuestion - 1];
            showCorrectAnswerDialog();
        } else {
            String correctAnswer = answerButtons[correctAnswerIndex].getText().toString();
            showWrongAnswerDialog(correctAnswer);
        }
    }

    private void showCorrectAnswerDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_correct_title)
            .setMessage(getString(R.string.dialog_correct_message, currentScore))
            .setPositiveButton(R.string.button_ok, (dialog, which) -> {
                currentQuestion++;
                if (currentQuestion > totalQuestions) {
                    showGameOverDialog();
                } else {
                    updateQuestion();
                }
            })
            .setCancelable(false)
            .show();
    }

    private void showWrongAnswerDialog(String correctAnswer) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_wrong_title)
            .setMessage(getString(R.string.dialog_wrong_message, correctAnswer, currentScore))
            .setPositiveButton(R.string.button_play_again, (dialog, which) -> resetGame())
            .setNegativeButton(R.string.button_exit, (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    private void showGameOverDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_game_over_title)
            .setMessage(getString(R.string.dialog_game_over_message, currentScore))
            .setPositiveButton(R.string.button_play_again, (dialog, which) -> resetGame())
            .setNegativeButton(R.string.button_exit, (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    private void useHint5050() {
        if (!hintsUsed[0]) {
            hintsUsed[0] = true;
            hint5050Button.setEnabled(false);
            // Hide two wrong answers
            int hiddenCount = 0;
            for (int i = 0; i < answerButtons.length && hiddenCount < 2; i++) {
                if (i != correctAnswerIndex) {
                    answerButtons[i].setVisibility(View.INVISIBLE);
                    hiddenCount++;
                }
            }
            showHintUsedDialog("50:50");
        }
    }

    private void useHintAudience() {
        if (!hintsUsed[1]) {
            hintsUsed[1] = true;
            hintAudienceButton.setEnabled(false);
            
            // Generate audience responses with preference for the correct answer
            StringBuilder audienceResponse = new StringBuilder();
            int correctPercentage = 40 + random.nextInt(40); // Between 40% and 80%
            int remainingPercentage = 100 - correctPercentage;
            
            int[] percentages = new int[4];
            
            // Assign the correct percentage
            percentages[correctAnswerIndex] = correctPercentage;
            
            // Distribute remaining percentage to wrong answers
            for (int i = 0; i < 4; i++) {
                if (i != correctAnswerIndex) {
                    int percentage = 0;
                    if (remainingPercentage > 0) {
                        percentage = random.nextInt(remainingPercentage + 1);
                        remainingPercentage -= percentage;
                    }
                    percentages[i] = percentage;
                }
            }
            
            // If there's any remaining percentage, add it to the last wrong answer
            if (remainingPercentage > 0) {
                for (int i = 3; i >= 0; i--) {
                    if (i != correctAnswerIndex) {
                        percentages[i] += remainingPercentage;
                        break;
                    }
                }
            }
            
            // Build the audience response message
            audienceResponse.append("Audience votes:\n");
            for (int i = 0; i < 4; i++) {
                char letter = (char) ('A' + i);
                audienceResponse.append(letter).append(": ").append(percentages[i]).append("%\n");
            }
            
            showHintUsedDialogWithMessage("Ask the Audience", audienceResponse.toString());
        }
    }

    private void useHintPhone() {
        if (!hintsUsed[2]) {
            hintsUsed[2] = true;
            hintPhoneButton.setEnabled(false);
            
            // 75% chance of getting the correct answer
            boolean givesCorrectAnswer = random.nextInt(100) < 75;
            
            StringBuilder phoneResponse = new StringBuilder("Your friend thinks the answer is: ");
            
            if (givesCorrectAnswer) {
                char letter = (char) ('A' + correctAnswerIndex);
                phoneResponse.append(letter);
                phoneResponse.append("\n\n\"I'm pretty sure it's ").append(letter).append("!\"");
            } else {
                // Give a wrong answer
                int wrongAnswer;
                do {
                    wrongAnswer = random.nextInt(4);
                } while (wrongAnswer == correctAnswerIndex);
                
                char letter = (char) ('A' + wrongAnswer);
                phoneResponse.append(letter);
                phoneResponse.append("\n\n\"I think it might be ").append(letter).append(", but I'm not 100% sure.\"");
            }
            
            showHintUsedDialogWithMessage("Phone a Friend", phoneResponse.toString());
        }
    }

    private void showHintUsedDialog(String hintName) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_hint_used_title)
            .setMessage(getString(R.string.dialog_hint_used_message, hintName))
            .setPositiveButton(R.string.button_ok, null)
            .show();
    }
    
    private void showHintUsedDialogWithMessage(String hintName, String additionalMessage) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_hint_used_title)
            .setMessage(getString(R.string.dialog_hint_used_message, hintName) + "\n\n" + additionalMessage)
            .setPositiveButton(R.string.button_ok, null)
            .show();
    }

    private void resetGame() {
        currentQuestion = 1;
        currentScore = 0;
        hintsUsed = new boolean[3];
        usedQuestionIndices.clear();
        
        hint5050Button.setEnabled(true);
        hintAudienceButton.setEnabled(true);
        hintPhoneButton.setEnabled(true);
        
        for (Button button : answerButtons) {
            button.setVisibility(View.VISIBLE);
        }
        
        updateQuestion();
    }
} 