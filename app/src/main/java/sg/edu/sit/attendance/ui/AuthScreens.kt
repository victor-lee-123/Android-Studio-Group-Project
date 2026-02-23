package sg.edu.sit.attendance.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import sg.edu.sit.attendance.auth.LocalAuthRepository
import sg.edu.sit.attendance.auth.LocalSession
import sg.edu.sit.attendance.data.DbProvider
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun LoginScreen(nav: NavController) {
    val ctx = LocalContext.current
    val db = remember { DbProvider.get(ctx) }
    val authRepo = remember { LocalAuthRepository(db.localAuthDao()) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
        Text("Attendance App", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            enabled = !loading,
            onClick = {
                loading = true
                error = null
                // Use LaunchedEffect-style coroutine
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    try {
                        val acct = authRepo.login(username, password)
                        LocalSession.save(ctx, acct.accountId)
                        nav.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "Login failed"
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Logging in..." else "Login")
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = { nav.navigate("signup") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }
    }
}

@Composable
fun SignUpScreen(nav: NavController) {
    val ctx = LocalContext.current
    val db = remember { DbProvider.get(ctx) }
    val authRepo = remember { LocalAuthRepository(db.localAuthDao()) }

    var username by remember { mutableStateOf("") }
    var studentName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
        Text("Create Account", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = studentName,
            onValueChange = { studentName = it },
            label = { Text("Student Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (min 6 chars)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            enabled = !loading,
            onClick = {
                loading = true
                error = null
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    try {
                        val acct = authRepo.signUp(username, password, studentName)
                        LocalSession.save(ctx, acct.accountId)
                        nav.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "Sign up failed"
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Creating..." else "Create Account")
        }

        Spacer(Modifier.height(10.dp))

        TextButton(onClick = { nav.popBackStack() }) {
            Text("Back to Login")
        }
    }
}