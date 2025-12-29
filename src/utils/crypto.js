import CryptoJS from 'crypto-js'

const BASE64_KEY = import.meta.env.VITE_APP_ENCRYPT_KEY || ''
const BASE64_IV = import.meta.env.VITE_APP_ENCRYPT_IV || ''

const KEY = CryptoJS.enc.Base64.parse(BASE64_KEY)
const IV = CryptoJS.enc.Base64.parse(BASE64_IV)

export function decrypt(word) {
    try {
        const decrypt = CryptoJS.AES.decrypt(word, KEY, {
            iv: IV,
            mode: CryptoJS.mode.CBC,
            padding: CryptoJS.pad.Pkcs7
        })

        const decryptedStr = decrypt.toString(CryptoJS.enc.Utf8)
        if (!decryptedStr) {
            throw new Error('Decryption failed - empty result')
        }

        return JSON.parse(decryptedStr)
    } catch (error) {
        console.error('Decrypt error:', error)
        throw new Error('Failed to decrypt data')
    }
}