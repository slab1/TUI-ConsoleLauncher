package ohi.andre.consolelauncher.commands.smartlauncher.test;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

/**
 * SmartLauncherTestActivity - UI for running and viewing test results
 * Provides a user interface to run the Smart Launcher test suite
 */
public class SmartLauncherTestActivity extends AppCompatActivity {
    
    private static final String TAG = "SmartLauncherTest";
    
    private TextView testOutput;
    private SmartLauncherTestSuite testSuite;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_launcher_test);
        
        testOutput = findViewById(R.id.test_output);
        
        // Initialize test suite
        testSuite = new SmartLauncherTestSuite(this);
        
        // Start tests
        runTests();
    }
    
    /**
     * Run all tests and display results
     */
    private void runTests() {
        new Thread(() -> {
            try {
                // Capture System.out for display
                java.io.PrintStream originalOut = System.out;
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                java.io.PrintStream ps = new java.io.PrintStream(baos);
                System.setOut(ps);
                
                // Run tests
                SmartLauncherTestSuite.TestReport report = testSuite.runAllTests();
                
                // Restore System.out
                System.setOut(originalOut);
                
                // Get test output
                String testOutput = baos.toString();
                
                // Display results
                runOnUiThread(() -> {
                    displayResults(testOutput, report);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Test execution failed", e);
                runOnUiThread(() -> {
                    testOutput.setText("Test execution failed: " + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * Display test results in the UI
     */
    private void displayResults(String output, SmartLauncherTestSuite.TestReport report) {
        StringBuilder result = new StringBuilder();
        
        // Add test output
        result.append("🧪 Smart Launcher Test Results\n");
        result.append("============================\n\n");
        result.append(output);
        result.append("\n\n");
        
        // Add summary
        result.append("📊 Summary:\n");
        result.append("---------\n");
        result.append("Total Tests: ").append(report.totalTests).append("\n");
        result.append("Passed: ").append(report.passedTests).append("\n");
        result.append("Failed: ").append(report.totalTests - report.passedTests).append("\n");
        result.append("Success Rate: ").append(String.format("%.1f%%", report.getSuccessRate())).append("\n\n");
        
        if (report.passedTests == report.totalTests) {
            result.append("🎉 All tests passed! Smart Launcher is ready.\n");
        } else {
            result.append("⚠️ Some tests failed. Check details above.\n");
        }
        
        // Add next steps
        result.append("\n🚀 Next Steps:\n");
        result.append("-------------\n");
        result.append("1. If all tests pass, proceed to AI configuration\n");
        result.append("2. Use: ai config <your_api_key> <your_group_id>\n");
        result.append("3. Test AI with: ai test\n");
        result.append("4. Try other commands like: calc, git, network\n");
        
        testOutput.setText(result.toString());
    }
    
    /**
     * Run specific test categories
     */
    public void runCategoryTests(String category) {
        // Implementation for running specific test categories
        // This could be expanded for more granular testing
    }
}