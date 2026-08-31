import { getBrowserType, getInstanceId, decodeURL } from './helpers.js'
import { sendHeartbeat } from './client.js'

export async function heartbeat(tab: chrome.tabs.Tab, tabCount: number): Promise<boolean> {
    const browserType = getBrowserType()
    const instanceId = await getInstanceId()
    return sendHeartbeat({
        url: decodeURL(tab.url ?? ''),
        title: tab.title ?? '',
        audible: tab.audible ?? false,
        incognito: tab.incognito ?? false,
        tabCount,
        browser: browserType,
        instanceId,
    })
}

export async function heartbeatFromCurrentTab(): Promise<boolean> {
    try {
        const tabs = await chrome.tabs.query({ active: true, currentWindow: true })
        const tab = tabs[0]
        if (!tab) return false
        const allTabs = await chrome.tabs.query({ currentWindow: true })
        return heartbeat(tab, allTabs.length)
    } catch {
        return false
    }
}
