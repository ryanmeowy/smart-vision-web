import CryptoJS from 'crypto-js'

const BASE64_KEY = import.meta.env.VITE_APP_ENCRYPT_KEY || ''
const BASE64_IV = import.meta.env.VITE_APP_ENCRYPT_IV || ''

const KEY = CryptoJS.enc.Base64.parse(BASE64_KEY)
const IV = CryptoJS.enc.Base64.parse(BASE64_IV)

export function decrypt(word) {
    const encryptedHexStr = CryptoJS.enc.Base64.parse(word)
    const srcs = CryptoJS.enc.Base64.stringify(encryptedHexStr)

    const decrypt = CryptoJS.AES.decrypt(srcs, KEY, {
        iv: IV,
        mode: CryptoJS.mode.CBC,
        padding: CryptoJS.pad.Pkcs7
    })

    const decryptedStr = decrypt.toString(CryptoJS.enc.Utf8)
    return JSON.parse(decryptedStr.toString())
}