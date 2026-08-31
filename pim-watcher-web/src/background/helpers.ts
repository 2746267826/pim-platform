export function getBrowserType(): string {
    const ua = navigator.userAgent
    if (ua.includes('Edg')) return 'edge'
    if (ua.includes('Chrome') && !ua.includes('Edg')) return 'chrome'
    if (ua.includes('Firefox')) return 'firefox'
    if (ua.includes('Safari') && !ua.includes('Chrome')) return 'safari'
    return 'other'
}

export async function getInstanceId(): Promise<string> {
    const anyBrowser: any = (globalThis as any).browser ?? (globalThis as any).chrome
    if (anyBrowser?.runtime?.id) return anyBrowser.runtime.id
    try {
        const stored: any = await anyBrowser?.storage?.local?.get?.('instanceId')
        if (stored?.instanceId) return stored.instanceId
        const uuid = crypto.randomUUID()
        await anyBrowser?.storage?.local?.set?.({ instanceId: uuid })
        return uuid
    } catch {
        const uuid = crypto.randomUUID()
        try { await anyBrowser?.storage?.local?.set?.({ instanceId: uuid }) } catch {}
        return uuid
    }
}

export function decodeURL(url: string): string {
    try { return decodeURIComponent(url) } catch { return url }
}
