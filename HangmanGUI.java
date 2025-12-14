package project_hangman;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

/**
 * Hangman Game – Java GUI (Swing)
 * Features:
 * - Single-player: Random word from list or "words.txt" (optional) in the same folder.
 * - Multiplayer: Player 1 enters the word, Player 2 guesses it.
 *   The entered words are saved to "words.txt" for future games.
 * - GUI with labels, text field, buttons.
 * - Validates single-letter A–Z guesses; ignores duplicates.
 * - Tracks wrong guesses and draws a hangman (7 stages).
 * - Shows guessed letters, remaining tries, win/lose status.
 * - Restart button to play again.
 */
public class HangmanGUI extends JFrame {

    // Game model state
    private String secretWord;
    private Set<Character> correctGuesses = new HashSet<>();
    private Set<Character> wrongGuesses = new LinkedHashSet<>();
    private int maxWrong = 6;

    // UI components
    private JLabel wordLabel;
    private JLabel statusLabel;
    private JLabel guessedLabel;
    private JTextField inputField;
    private JButton guessButton;
    private JButton restartButton;
    private JButton multiplayerButton; // NEW
    private HangmanCanvas canvas;

    // Word source
    private final List<String> defaultWords = Arrays.asList(
            "JAVA","SWING","OBJECT","INHERITANCE","ENCAPSULATION",
            "POLYMORPHISM","ALGORITHM","COMPILE","DEBUG","EXCEPTION",
            "THREAD","INTERFACE","ABSTRACTION","COLLECTION","GENERIC",
            "MAVEN","GRADLE","PACKAGE","LAMBDA","STREAM"
    );

    public HangmanGUI() {
        super("Hangman – Java Swing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        // Canvas (drawing area)
        canvas = new HangmanCanvas();
        canvas.setPreferredSize(new Dimension(380, 300));
        add(canvas, BorderLayout.WEST);

        // Right panel for controls and info
        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(right, BorderLayout.CENTER);

        // Top: Word display
        wordLabel = new JLabel("", SwingConstants.CENTER);
        wordLabel.setFont(new Font("Consolas", Font.BOLD, 28));
        right.add(wordLabel, BorderLayout.NORTH);

        // Center: Input + guessed letters
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        inputField = new JTextField(4);
        inputField.setFont(new Font("Consolas", Font.PLAIN, 22));
        guessButton = new JButton("Guess");
        inputRow.add(new JLabel("Enter a letter:"));
        inputRow.add(inputField);
        inputRow.add(guessButton);
        center.add(inputRow);

        guessedLabel = new JLabel("Guessed (wrong): –");
        guessedLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        center.add(guessedLabel);

        right.add(center, BorderLayout.CENTER);

        // Bottom: status + restart + multiplayer
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        statusLabel = new JLabel("Welcome! Make a guess.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        bottom.add(statusLabel, BorderLayout.CENTER);

        restartButton = new JButton("Restart");
        bottom.add(restartButton, BorderLayout.EAST);

        // NEW: Multiplayer button
        multiplayerButton = new JButton("Multiplayer");
        bottom.add(multiplayerButton, BorderLayout.WEST);

        right.add(bottom, BorderLayout.SOUTH);

        // Wire events
        guessButton.addActionListener(e -> onGuess());
        inputField.addActionListener(e -> onGuess());
        restartButton.addActionListener(e -> startNewGame());
        multiplayerButton.addActionListener(e -> startMultiplayerSetup()); // NEW

        // Start game
        startNewGame();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /** Start/restart a single-player game (random word) */
    private void startNewGame() {
        correctGuesses.clear();
        wrongGuesses.clear();
        secretWord = pickWord().toUpperCase(Locale.ROOT);
        updateWordLabel();
        updateGuessedLabel();
        statusLabel.setText("New game started. You have " + (maxWrong - wrongGuesses.size()) + " tries.");
        canvas.setWrongCount(0);
        setControlsEnabled(true);
        inputField.requestFocusInWindow();
        inputField.setText("");
    }

    /** Start a multiplayer game with a specific word */
    private void startNewGameWithWord(String word) {
        correctGuesses.clear();
        wrongGuesses.clear();
        secretWord = word.toUpperCase(Locale.ROOT);
        updateWordLabel();
        updateGuessedLabel();
        statusLabel.setText("Multiplayer game! Player 2, start guessing. " +
                (maxWrong - wrongGuesses.size()) + " tries.");
        canvas.setWrongCount(0);
        setControlsEnabled(true);
        inputField.requestFocusInWindow();
        inputField.setText("");
    }

    /** Multiplayer setup: Player 1 enters the word, save it, and start game */
    private void startMultiplayerSetup() {
        String word = JOptionPane.showInputDialog(
                this,
                "Player 1: Enter the secret word (letters only):",
                "Multiplayer – Set Secret Word",
                JOptionPane.PLAIN_MESSAGE
        );

        if (word == null) { // cancel pressed
            statusLabel.setText("Multiplayer cancelled.");
            return;
        }

        word = word.trim();

        if (word.isEmpty() || !word.matches("[A-Za-z]+")) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a word using letters A–Z only.",
                    "Invalid word",
                    JOptionPane.ERROR_MESSAGE
            );
            statusLabel.setText("Invalid secret word. Try Multiplayer again.");
            return;
        }

        word = word.toUpperCase(Locale.ROOT);

        // Save this custom word into words.txt for future games
        saveCustomWord(word);

        // Optional: simple "reverify" – show confirmation to Player 1
        JOptionPane.showMessageDialog(
                this,
                "Secret word saved!\nHand over to Player 2 to start guessing.",
                "Word Saved",
                JOptionPane.INFORMATION_MESSAGE
        );

        // Start multiplayer game with this word
        startNewGameWithWord(word);
    }

    /** Append custom word to words.txt */
    private void saveCustomWord(String word) {
        File f = new File("words.txt");
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(f, true), StandardCharsets.UTF_8))) {
            bw.write(word);
            bw.newLine();
        } catch (IOException ex) {
            // Not critical, just log it
            System.err.println("Could not save word to words.txt: " + ex.getMessage());
        }
    }

    /** Load word list from words.txt if present; else use defaults */
    private String pickWord() {
        List<String> words = new ArrayList<>();
        File f = new File("words.txt");
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && line.matches("[A-Za-z]+")) {
                        words.add(line.toUpperCase(Locale.ROOT));
                    }
                }
            } catch (IOException ignored) {}
        }
        if (words.isEmpty()) words = defaultWords;
        return words.get(new Random().nextInt(words.size()));
    }

    /** Handle a guess from the input field */
    private void onGuess() {
        String text = inputField.getText().trim().toUpperCase(Locale.ROOT);
        inputField.setText("");
        if (text.length() != 1 || !text.matches("[A-Z]")) {
            statusLabel.setText("Please enter a single letter (A–Z).");
            return;
        }
        char c = text.charAt(0);

        if (correctGuesses.contains(c) || wrongGuesses.contains(c)) {
            statusLabel.setText("You already tried '" + c + "'. Try a different letter.");
            return;
        }

        if (secretWord.indexOf(c) >= 0) {
            correctGuesses.add(c);
            statusLabel.setText("Good! '" + c + "' is in the word.");
        } else {
            wrongGuesses.add(c);
            statusLabel.setText("Nope! '" + c + "' is not in the word. " +
                    (maxWrong - wrongGuesses.size()) + " tries left.");
            canvas.setWrongCount(wrongGuesses.size());
        }

        updateWordLabel();
        updateGuessedLabel();

        if (isWin()) {
            statusLabel.setText("You Win! The word was \"" + secretWord + "\". Press Restart to play again.");
            setControlsEnabled(false);
        } else if (wrongGuesses.size() >= maxWrong) {
            statusLabel.setText("Game Over! The word was \"" + secretWord + "\". Press Restart to try again.");
            revealAll();
            setControlsEnabled(false);
        } else {
            inputField.requestFocusInWindow();
        }
    }

    private void setControlsEnabled(boolean enabled) {
        guessButton.setEnabled(enabled);
        inputField.setEnabled(enabled);
    }

    private boolean isWin() {
        for (char ch : secretWord.toCharArray()) {
            if (Character.isLetter(ch) && !correctGuesses.contains(ch)) {
                return false;
            }
        }
        return true;
    }

    /** Reveal whole word on game over (visual only) */
    private void revealAll() {
        // Add all letters so the display shows the whole word
        for (char ch : secretWord.toCharArray()) {
            if (Character.isLetter(ch)) correctGuesses.add(ch);
        }
        updateWordLabel();
    }

    private void updateGuessedLabel() {
        StringBuilder sb = new StringBuilder();
        if (!wrongGuesses.isEmpty()) {
            for (char ch : wrongGuesses) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(ch);
            }
        } else {
            sb.append("–");
        }
        guessedLabel.setText("Guessed (wrong): " + sb);
    }

    private void updateWordLabel() {
        StringBuilder sb = new StringBuilder();
        for (char ch : secretWord.toCharArray()) {
            if (Character.isLetter(ch)) {
                sb.append(correctGuesses.contains(ch) ? ch : '_');
            } else {
                sb.append(ch); // keep any non-letters (e.g., hyphens/spaces)
            }
            sb.append(' ');
        }
        wordLabel.setText(sb.toString().trim());
    }

    /** Canvas panel to draw hangman stages based on wrong guesses */
    private static class HangmanCanvas extends JPanel {
        private int wrongCount = 0; // 0..6

        public void setWrongCount(int n) {
            this.wrongCount = Math.max(0, Math.min(6, n));
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(380, 300);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            g2.setStroke(new BasicStroke(3f));

            // Ground/base
            int baseY = h - 30;
            g2.drawLine(40, baseY, 180, baseY);   // base
            g2.drawLine(80, baseY, 80, 40);       // pole
            g2.drawLine(78, 40, 200, 40);         // top beam
            g2.drawLine(198, 40, 198, 70);        // rope

            // Draw parts progressively
            if (wrongCount >= 1) { // head
                g2.drawOval(180, 70, 36, 36);
            }
            if (wrongCount >= 2) { // body
                g2.drawLine(198, 106, 198, 170);
            }
            if (wrongCount >= 3) { // left arm
                g2.drawLine(198, 120, 168, 145);
            }
            if (wrongCount >= 4) { // right arm
                g2.drawLine(198, 120, 228, 145);
            }
            if (wrongCount >= 5) { // left leg
                g2.drawLine(198, 170, 175, 210);
            }
            if (wrongCount >= 6) { // right leg
                g2.drawLine(198, 170, 221, 210);
            }

            g2.dispose();
        }
    }

    public static void main(String[] args) {
        // Optional: set a clean look & feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(HangmanGUI::new);
    }
}
