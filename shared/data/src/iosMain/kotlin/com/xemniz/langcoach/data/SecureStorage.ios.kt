package com.xemniz.langcoach.data

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.CFRetain
import platform.Foundation.CFBridgingRetain
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSCopyingProtocol
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class SecureStorage {
    private val service = "com.xemniz.langcoach"

    actual suspend fun save(key: String, value: String) {
        delete(key)
        val query = baseQuery(key).apply {
            setObject(
                NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding)!!,
                forKey = cfKey(kSecValueData),
            )
            setObject(
                cfObject(kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly),
                forKey = cfKey(kSecAttrAccessible),
            )
        }
        val status = query.useAsCfDictionary { SecItemAdd(it, null) }
        checkStatus(status, "save", key)
    }

    actual suspend fun read(key: String): String? = memScoped {
        val query = baseQuery(key).apply {
            setObject(true, forKey = cfKey(kSecReturnData))
        }
        val result = alloc<CFTypeRefVar>()
        val status = query.useAsCfDictionary { SecItemCopyMatching(it, result.ptr) }
        when (status) {
            errSecSuccess -> {
                val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
                NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
            }
            errSecItemNotFound -> null
            else -> error("Keychain read failed for '$key' (OSStatus $status)")
        }
    }

    actual suspend fun delete(key: String) {
        val status = baseQuery(key).useAsCfDictionary { SecItemDelete(it) }
        when (status) {
            errSecSuccess, errSecItemNotFound -> Unit
            else -> error("Keychain delete failed for '$key' (OSStatus $status)")
        }
    }

    private fun baseQuery(key: String) = NSMutableDictionary().apply {
        setObject(cfObject(kSecClassGenericPassword), forKey = cfKey(kSecClass))
        setObject(service, forKey = cfKey(kSecAttrService))
        setObject(key, forKey = cfKey(kSecAttrAccount))
    }

    private inline fun <T> NSMutableDictionary.useAsCfDictionary(
        block: (CFDictionaryRef) -> T,
    ): T {
        val dictionary = CFBridgingRetain(this) as CFDictionaryRef
        return try {
            block(dictionary)
        } finally {
            CFBridgingRelease(dictionary)
        }
    }

    private fun cfKey(value: CFStringRef?): NSCopyingProtocol =
        cfObject(value) as NSCopyingProtocol

    private fun cfObject(value: CFStringRef?): Any =
        CFBridgingRelease(CFRetain(requireNotNull(value)))!!

    private fun checkStatus(status: Int, operation: String, key: String) {
        check(status == errSecSuccess) {
            "Keychain $operation failed for '$key' (OSStatus $status)"
        }
    }
}
