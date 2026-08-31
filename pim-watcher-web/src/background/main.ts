import { heartbeatFromCurrentTab } from './heartbeat.js'
import { waitForPimClient } from './client.js'

const ALARM_NAME = 'pim-heartbeat'

async function onAlarm(alarm: chrome.alarms.Alarm) {
    if (alarm.name !== ALARM_NAME) return
    await heartbeatFromCurrentTab()
}

async function init() {
    await waitForPimClient(3)
    chrome.alarms.onAlarm.addListener(onAlarm)
    try { chrome.alarms.create(ALARM_NAME, { periodInMinutes: 0.5 }) } catch {}
    chrome.tabs.onActivated.addListener(() => { void heartbeatFromCurrentTab() })
    chrome.tabs.onUpdated.addListener((_tabId, changeInfo) => {
        if (changeInfo.url || changeInfo.title || changeInfo.audible !== undefined) {
            void heartbeatFromCurrentTab()
        }
    })
    await heartbeatFromCurrentTab()
}

void init()
