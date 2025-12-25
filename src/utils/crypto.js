import CryptoJS from 'crypto-js'

const RAW_KEY = import.meta.env.VITE_APP_ENCRYPT_KEY || ''
const RAW_IV = import.meta.env.VITE_APP_ENCRYPT_IV || ''


export function decrypt(word) {
    const KEY = CryptoJS.enc.Utf8.parse(RAW_KEY) // 16位
    const IV = CryptoJS.enc.Utf8.parse(RAW_IV)  // 16位

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