package sg.edu.sit.attendance.auth

import android.content.Context
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@Entity(
    tableName = "local_accounts",
    indices = [Index(value = ["username"], unique = true)]
)
data class LocalAccountEntity(
    @PrimaryKey val accountId: String = UUID.randomUUID().toString(),
    val username: String,
    val studentName: String,
    val passwordHashB64: String,
    val saltB64: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Dao
interface LocalAuthDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: LocalAccountEntity)

    @Query("SELECT * FROM local_accounts WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): LocalAccountEntity?

    @Query("SELECT * FROM local_accounts WHERE accountId = :id LIMIT 1")
    suspend fun findById(id: String): LocalAccountEntity?
}

object PasswordHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    fun newSalt(bytes: Int = 16): ByteArray =
        ByteArray(bytes).also { SecureRandom().nextBytes(it) }

    fun hash(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    fun unb64(s: String): ByteArray = Base64.getDecoder().decode(s)

    fun verify(password: CharArray, saltB64: String, hashB64: String): Boolean {
        val salt = unb64(saltB64)
        val expected = unb64(hashB64)
        val actual = hash(password, salt)
        return actual.contentEquals(expected)
    }
}

class LocalAuthRepository(
    private val dao: LocalAuthDao
) {
    suspend fun signUp(username: String, password: String, studentName: String): LocalAccountEntity {
        val u = username.trim()
        val n = studentName.trim()

        require(u.isNotEmpty()) { "Username cannot be empty" }
        require(n.isNotEmpty()) { "Student name cannot be empty" }
        require(password.length >= 6) { "Password must be at least 6 characters" }

        val existing = dao.findByUsername(u)
        require(existing == null) { "Username already exists" }

        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash(password.toCharArray(), salt)

        val account = LocalAccountEntity(
            username = u,
            studentName = n,
            passwordHashB64 = PasswordHasher.b64(hash),
            saltB64 = PasswordHasher.b64(salt)
        )

        dao.insert(account)
        return account
    }

    suspend fun login(username: String, password: String): LocalAccountEntity {
        val u = username.trim()
        val account = dao.findByUsername(u)
            ?: throw IllegalArgumentException("Invalid username or password")

        val ok = PasswordHasher.verify(password.toCharArray(), account.saltB64, account.passwordHashB64)
        if (!ok) throw IllegalArgumentException("Invalid username or password")

        return account
    }
}

object LocalSession {
    private const val PREF = "local_auth"
    private const val KEY_ACCOUNT_ID = "account_id"

    fun save(context: Context, accountId: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCOUNT_ID, accountId)
            .apply()
    }

    fun get(context: Context): String? =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_ACCOUNT_ID, null)

    fun logout(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACCOUNT_ID)
            .apply()
    }
}