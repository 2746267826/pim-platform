import { createWriteStream } from 'fs'
import { readdir, stat } from 'fs/promises'
import { join, relative } from 'path'
import { createRequire } from 'module'
let archiver
try { archiver = createRequire(import.meta.url)('archiver') } catch {}
const dist = 'dist'
const out = 'dist/pim-browser-extension.zip'
async function collect(dir, base) {
  const entries = await readdir(dir, { withFileTypes: true })
  let files = []
  for (const e of entries) {
    const p = join(dir, e.name)
    if (e.isDirectory()) files = files.concat(await collect(p, base))
    else files.push(relative(base, p))
  }
  return files
}
if (!archiver) {
  // fallback: use system zip if available
  const { execSync } = await import('child_process')
  try {
    execSync(`cd ${dist} && zip -r ../${out} . > /dev/null`, { stdio: 'inherit' })
    console.log(`✅ ${out} via zip`)
  } catch (e) {
    console.log('zip fallback failed, skipping archive creation')
  }
} else {
  const output = createWriteStream(join(process.cwd(), out))
  const archive = archiver('zip', { zlib: { level: 9 } })
  archive.pipe(output)
  const files = await collect(dist, dist)
  for (const f of files) archive.file(join(dist, f), { name: f })
  await archive.finalize()
  console.log(`✅ ${out}`)
}
