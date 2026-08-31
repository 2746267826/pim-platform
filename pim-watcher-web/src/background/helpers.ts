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
    try {
        const stored: any = await anyBrowser?.storage?.local?.get?.('instanceId')
        if (stored?.instanceId) return stored.instanceId
    } catch {}
    try {
        const uuid = crypto.randomUUID()
        const baseId = anyBrowser?.runtime?.id ? `${anyBrowser.runtime.id}_${uuid.slice(0, 8)}` : uuid
        await anyBrowser?.storage?.local?.set?.({ instanceId: baseId })
        return baseId
    } catch {
        const uuid = crypto.randomUUID()
        return anyBrowser?.runtime?.id ? `${anyBrowser.runtime.id}_${uuid.slice(0, 8)}` : uuid
    }
}

export function decodeURL(url: string): string {
    try { return decodeURIComponent(url) } catch { return url }
}
