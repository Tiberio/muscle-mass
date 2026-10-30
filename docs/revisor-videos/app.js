/* Meu Treino — revisão manual de vídeos (sem backend e sem bibliotecas externas). */
(() => {
'use strict';
const dataset = JSON.parse(document.getElementById('dataset').textContent);
const ITEMS = dataset.items;
const STORE_KEY = 'meutreino:video-review:2026-10:1';
const byId = Object.fromEntries(ITEMS.map(x => [x.id, x]));
let saved = {};
try { saved = JSON.parse(localStorage.getItem(STORE_KEY)) || {}; } catch (_) { saved = {}; }
let reviews = saved.reviews && typeof saved.reviews === 'object' ? saved.reviews : {};
let drafts = saved.drafts && typeof saved.drafts === 'object' ? saved.drafts : {};
let currentId = ITEMS[0].id;
let currentFilter = 'pending';
let term = '';
let clipPreviewedFor = '';
const $ = id => document.getElementById(id);
const esc = s => String(s == null ? '' : s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const clock = secs => `${Math.floor(secs / 60)}:${String(Math.floor(secs % 60)).padStart(2, '0')}`;
const persist = () => { try {localStorage.setItem(STORE_KEY, JSON.stringify({reviews,drafts}));} catch (_) {alert('O navegador não permitiu salvar localmente. Exporte suas decisões antes de sair.');} };
const status = id => reviews[id]?.decision || 'pending';
const count = state => ITEMS.filter(v => status(v.id) === state).length;
const idx = id => ITEMS.findIndex(v => v.id === id);
const validClip = (v, draft) => {
  if (!draft) return false;
  const s = Number(draft.start), e = Number(draft.end);
  return draft.start !== '' && draft.end !== '' && Number.isInteger(s) && Number.isInteger(e) && s >= 0 && e > s && e <= v.duration && e - s <= 40;
};
function filterItems() {
  return ITEMS.filter(v => {
    const matches = !term || [v.pt, v.en, v.id, v.youtubeTitle].some(x => x.toLocaleLowerCase('pt-BR').includes(term));
    if (!matches) return false;
    if (currentFilter === 'all') return true;
    if (currentFilter === 'long') return v.duration > 40;
    return status(v.id) === currentFilter;
  });
}
function renderCounts() {
  const approved = count('approved'), rejected = count('rejected');
  $('approvedCount').textContent = approved;
  $('rejectedCount').textContent = rejected;
  $('pendingCount').textContent = 60 - approved - rejected;
  $('progressText').textContent = `${approved + rejected} / 60 revisados`;
  $('progressFill').style.width = `${(approved + rejected) / 60 * 100}%`;
}
function renderList() {
  const list = filterItems();
  $('visibleCount').textContent = `${list.length} exercícios`;
  if (!list.length) { $('list').innerHTML = '<p class="empty">Nenhum exercício neste filtro. Escolha “Todos” para rever decisões.</p>'; return; }
  $('list').innerHTML = list.map(v => {
    const st = status(v.id), index = idx(v.id) + 1;
    const dot = st === 'approved' ? '✓' : st === 'rejected' ? '✕' : '○';
    return `<button type="button" class="nav-item ${v.id === currentId ? 'current' : ''}" data-id="${esc(v.id)}" aria-label="Abrir ${esc(v.pt)}">
      <span class="nav-num">${String(index).padStart(2,'0')}</span><span style="min-width:0;flex:1"><span class="nav-title">${esc(v.pt)}</span><span class="nav-sub">${esc(v.en)} · ${clock(v.duration)}</span></span><span class="nav-status ${st==='approved'?'ok':st==='rejected'?'no':'wait'}">${dot}</span></button>`;
  }).join('');
  $('list').querySelectorAll('[data-id]').forEach(b => b.addEventListener('click', () => select(b.dataset.id)));
}
function stopPlayer() { $('screen').innerHTML = ''; }
function showPoster(v) {
  stopPlayer();
  const box = document.createElement('div'); box.className = 'poster';
  box.style.setProperty('--thumb', `url("https://i.ytimg.com/vi/${v.youtubeId}/hqdefault.jpg")`);
  const button = document.createElement('button');button.className='play';button.type='button';button.title='Reproduzir vídeo inteiro';button.textContent='▶';
  button.addEventListener('click',() => loadPlayer(v, null));
  const message=document.createElement('span');message.textContent='Clique para examinar o vídeo';
  box.append(button,message);$('screen').append(box);
}
function loadPlayer(v, clip) {
  const pars = new URLSearchParams({playsinline:'1',controls:'1',rel:'0',autoplay:'1'});
  if (clip) { pars.set('start',String(clip.start)); pars.set('end',String(clip.end));clipPreviewedFor = `${v.id}:${clip.start}:${clip.end}`; }
  const iframe = document.createElement('iframe');
  iframe.src = `https://www.youtube-nocookie.com/embed/${v.youtubeId}?${pars.toString()}`;
  iframe.title = 'Demonstração de ' + v.pt;
  iframe.allow = 'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share';
  iframe.allowFullscreen = true;
  iframe.referrerPolicy = 'strict-origin-when-cross-origin';
  stopPlayer(); $('screen').append(iframe);
}
function linkForVideo(v) { return `https://www.youtube.com/watch?v=${encodeURIComponent(v.youtubeId)}`; }
function onClipChange() {
  const v = byId[currentId];
  const draft = {start:$('start').value, end:$('end').value};
  drafts[v.id] = draft; clipPreviewedFor = '';persist();
  const valid = validClip(v, draft);
  $('clipValidity').textContent = valid ? `Trecho selecionado: ${clock(Number(draft.start))}–${clock(Number(draft.end))} (${Number(draft.end) - Number(draft.start)} s). Reveja o trecho antes de aprovar.` : 'Informe início e fim (segundos). O intervalo deve ter no máximo 40 segundos.';
  $('clipValidity').style.color = valid ? '#7ce7c2' : '#ffcf81';
  $('previewClip').disabled = !valid;
}
function select(id, shouldScroll = true) {
  const v = byId[id]; if (!v) return;
  currentId=id;clipPreviewedFor='';
  $('counter').textContent = `Exercício ${idx(id)+1} de ${ITEMS.length}`;
  $('namePt').textContent=v.pt; $('nameEn').textContent=v.en;
  $('description').textContent = v.description;
  $('muscle').textContent = v.muscle || '—';
  $('duration').textContent=clock(v.duration);
  $('videoTitle').textContent=v.youtubeTitle;
  $('channel').textContent=v.channel || '—';
  $('videoLink').href=linkForVideo(v);
  $('statusChip').className='chip ' + (status(id)==='approved'?'ok':status(id)==='rejected'?'no':'');
  $('statusChip').textContent = status(id)==='approved'?'✓ Aprovado':status(id)==='rejected'?'✕ Rejeitado':'Pendente de revisão';
  $('durationChip').textContent=v.duration<=40?'Vídeo inteiro · até 40 s':'Vídeo longo · recorte necessário';
  $('durationChip').className = 'chip ' + (v.duration>40?'warn':'');
  $('warning').style.display = v.warnings.length?'block':'none';
  $('warning').textContent=v.warnings.join(' · ');
  $('clipbox').style.display = v.duration>40?'block':'none';
  $('entire').textContent = v.duration>40?'▶ Assistir ao original para localizar o trecho':'▶ Reproduzir vídeo inteiro';
  if (v.duration>40) {
    const draft = drafts[v.id] || {};
    $('start').value = draft.start ?? '';
    $('end').value = draft.end ?? '';
    $('start').max = String(v.duration);
    $('end').max = String(v.duration);
    onClipChange();
  }
  const rev=reviews[v.id]||{};
  $('reason').value=rev.reason || '';
  $('observation').value=rev.note || '';
  $('reviewState').textContent = rev.decision ? `Última decisão: ${rev.decision==='approved'?'aprovado':'rejeitado'} (${new Date(rev.reviewed_at).toLocaleString('pt-BR')})` : 'Ainda não revisado';
  showPoster(v);
  renderCounts();renderList();
  if (shouldScroll) $('maincard').scrollIntoView({behavior:'smooth',block:'start'});
}
function nextUnreviewed(afterId) {
  const start=idx(afterId);
  for(let n=1;n<=ITEMS.length;n++) {
    const v=ITEMS[(start+n)%ITEMS.length];
    if(status(v.id)==='pending') return v.id;
  }
  return null;
}
function decide(decision) {
  const v = byId[currentId];
  const draft = drafts[v.id] || {};
  let start_seconds=null, end_seconds=null;
  if(decision==='approved' && v.duration>40) {
    if(!validClip(v,draft)) { alert('Para vídeos com mais de 40 segundos, selecione um trecho de até 40 segundos antes de aprovar.');$('start').focus();return; }
    start_seconds = Number(draft.start);end_seconds=Number(draft.end);
    if(clipPreviewedFor !== `${v.id}:${start_seconds}:${end_seconds}`) {alert('Reproduza o trecho selecionado antes de aprová-lo.');return;}
  }
  reviews[v.id]={ decision, youtube_id:v.youtubeId, start_seconds, end_seconds,
    reason:decision==='rejected'?$('reason').value:'', note:$('observation').value.trim(),reviewed_at:new Date().toISOString() };
  persist();
  const next=nextUnreviewed(v.id);
  if(next){select(next)} else {select(v.id); if(ITEMS.every(x=>status(x.id)!=='pending')) alert('Os 60 vídeos foram revisados! Exporte o JSON das decisões para integrar o resultado ao catálogo.');}
}
function navigate(dir) {let n = idx(currentId);n=(n+dir+ITEMS.length)%ITEMS.length;select(ITEMS[n].id);}
function exportFile(format) {
  const results=ITEMS.filter(v=>status(v.id)!=='pending').map(v=>({
    exercise_id:v.id, exercise_name_pt:v.pt, exercise_name_en:v.en, youtube_id:v.youtubeId,
    duration_seconds:v.duration, ...reviews[v.id]
  }));
  const summary={approved:count('approved'),rejected:count('rejected'),pending:ITEMS.length-results.length,total:ITEMS.length};
  let payload,mime,filename;
  if(format==='csv') {
    const cols=['exercise_id','exercise_name_pt','exercise_name_en','youtube_id','duration_seconds','decision','start_seconds','end_seconds','reason','note','reviewed_at'];
    const quote=v=>`"${String(v==null?'':v).replace(/"/g,'""')}"`;
    payload='\ufeff'+[cols.join(';'),...results.map(r=>cols.map(k=>quote(r[k])).join(';'))].join('\r\n');mime='text/csv;charset=utf-8';filename='revisao_60_videos_meutreino.csv';
  } else {
    payload=JSON.stringify({schema_version:1,project:'Meu Treino v0.4.5',source:'Auditoria-60-videos-MeuTreino',exported_at:new Date().toISOString(),summary,results},null,2);
    mime='application/json';filename='revisao_60_videos_meutreino.json';
  }
  const a=document.createElement('a');const url=URL.createObjectURL(new Blob([payload],{type:mime}));a.href=url;a.download=filename;document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);
}
function importData(file) {
  if(!file)return;
  const reader=new FileReader();reader.onload=()=>{
    try{
      const obj=JSON.parse(reader.result);
      if(!Array.isArray(obj.results)||!obj.results.every(r=>r&&byId[r.exercise_id]&&['approved','rejected'].includes(r.decision)))throw Error('Formato de revisão incompatível');
      const incoming={};for(const r of obj.results) {
        const v=byId[r.exercise_id];if(r.youtube_id !== v.youtubeId) throw Error(`Vídeo divergente: ${v.pt}`);
        if(r.decision==='approved'&&v.duration>40&&!validClip(v,{start:r.start_seconds,end:r.end_seconds}))throw Error(`Trecho inválido: ${v.pt}`);
        incoming[v.id]={decision:r.decision,youtube_id:r.youtube_id,start_seconds:r.start_seconds??null,end_seconds:r.end_seconds??null,reason:r.reason||'',note:r.note||'',reviewed_at:r.reviewed_at||new Date().toISOString()};
        if(r.start_seconds!=null && r.end_seconds!=null)drafts[v.id]={start:r.start_seconds,end:r.end_seconds};
      }
      if(!confirm(`Importar ${Object.keys(incoming).length} decisões? As decisões dos mesmos exercícios serão substituídas.`))return;
      reviews={...reviews,...incoming};persist();select(currentId);
    }catch(err){alert('Não foi possível importar: '+err.message)}
  };reader.readAsText(file);
}
$('search').addEventListener('input',e=>{term=e.target.value.toLocaleLowerCase('pt-BR').trim();renderList()});
document.querySelectorAll('[data-filter]').forEach(button=>button.addEventListener('click',()=>{
 currentFilter=button.dataset.filter;
 document.querySelectorAll('[data-filter]').forEach(b=>b.classList.toggle('active',b===button));
 renderList();
}));
$('entire').addEventListener('click',()=>loadPlayer(byId[currentId],null));
$('previewClip').addEventListener('click',()=>{const v=byId[currentId],d=drafts[v.id];if(!validClip(v,d))return;loadPlayer(v,{start:Number(d.start),end:Number(d.end)})});
$('start').addEventListener('input',onClipChange);$('end').addEventListener('input',onClipChange);
$('approve').addEventListener('click',()=>decide('approved'));
$('reject').addEventListener('click',()=>decide('rejected'));
$('clear').addEventListener('click',()=>{if(!reviews[currentId])return;if(!confirm('Remover a decisão deste vídeo e deixá-lo pendente?'))return;delete reviews[currentId];persist();select(currentId)});
$('prev').addEventListener('click',()=>navigate(-1));$('next').addEventListener('click',()=>navigate(1));
$('exportJson').addEventListener('click',()=>exportFile('json'));
$('exportCsv').addEventListener('click',()=>exportFile('csv'));
$('importBtn').addEventListener('click',()=>$('importFile').click());
$('importFile').addEventListener('change',e=>{importData(e.target.files?.[0]);e.target.value='';});
document.addEventListener('keydown',e=>{if(['INPUT','TEXTAREA','SELECT'].includes(document.activeElement.tagName))return;if(e.key==='ArrowRight')navigate(1);if(e.key==='ArrowLeft')navigate(-1)});
const first=ITEMS.find(v=>status(v.id)==='pending')||ITEMS[0]; select(first.id, false);
})();
