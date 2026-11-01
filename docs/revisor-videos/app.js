/* Meu Treino — mesmo revisor clássico, agora com os 130 exercícios. */
(() => {
'use strict';
const dataset = JSON.parse(document.getElementById('dataset').textContent);
const seed = JSON.parse(document.getElementById('seedReviews').textContent);
const ITEMS = dataset.items;
const STORE_KEY = 'meutreino:video-review:classic-130:2026-10';
const byId = Object.fromEntries(ITEMS.map(x => [x.id, x]));
let saved = {};
try { saved = JSON.parse(localStorage.getItem(STORE_KEY)) || {}; } catch (_) { saved = {}; }
let reviews = saved.reviews && typeof saved.reviews==='object' ? saved.reviews : {};
let drafts = saved.drafts && typeof saved.drafts==='object' ? saved.drafts : {};
let choices = saved.choices && typeof saved.choices==='object' ? saved.choices : {};
let skipped = saved.skipped && typeof saved.skipped==='object' ? saved.skipped : {};
const $=id=>document.getElementById(id);
const esc=s=>String(s==null?'':s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const clock=secs=>`${Math.floor(secs/60)}:${String(Math.floor(secs%60)).padStart(2,'0')}`;
const validClip=(v,d)=>!!d&&d.start!==''&&d.end!==''&&d.start!=null&&d.end!=null&&Number.isInteger(Number(d.start))&&Number.isInteger(Number(d.end))&&Number(d.start)>=0&&Number(d.end)>Number(d.start)&&Number(d.end)<=v.duration&&(Number(d.end)-Number(d.start)<=40);
// Atualização dos dados de revisão, sem modificar a interface.
// Executa apenas uma vez por versão; decisões mais recentes no navegador prevalecem.
const SEED_VERSION = 'review-130-49approved-1rejected-2026-11-01';
if(saved.seedVersion !== SEED_VERSION){
 for(const r of seed.results){
  const ex=byId[r.exercise_id];
  if(!ex || !['approved','rejected'].includes(r.decision))continue;
  const optionIndex=ex.options.findIndex(o=>o.id===r.youtube_id);
  if(r.decision==='approved' && optionIndex<0)continue;
  const previous=reviews[ex.id];
  if(previous && (Date.parse(previous.reviewed_at)||0)>(Date.parse(r.reviewed_at)||0))continue;
  reviews[ex.id]={decision:r.decision,youtube_id:r.youtube_id??null,start_seconds:r.start_seconds??null,end_seconds:r.end_seconds??null,reason:r.reason||'',note:r.note||'',reviewed_at:r.reviewed_at||new Date().toISOString()};
  if(optionIndex>=0)choices[ex.id]=optionIndex;
  if(r.decision==='approved' && r.start_seconds!=null && r.end_seconds!=null){
   (drafts[ex.id]??={})[r.youtube_id]={start:String(r.start_seconds),end:String(r.end_seconds)};
  }
 }
 for(const [id,option] of Object.entries(seed.choices||{})){
  if(byId[id] && choices[id]===undefined && Number.isInteger(option) && option>=0 && option<byId[id].options.length)choices[id]=option;
 }
 for(const [id,clips] of Object.entries(seed.drafts||{})){
  if(!byId[id])continue;
  drafts[id]??={};
  for(const [vid,clip] of Object.entries(clips)){
   if(byId[id].options.some(o=>o.id===vid) && !drafts[id][vid])drafts[id][vid]=clip;
  }
 }
 for(const [id,discarded] of Object.entries(seed.skipped||{})){
  if(!byId[id])continue;
  skipped[id]=Array.from(new Set([...(skipped[id]||[]),...discarded.filter(vid=>byId[id].options.some(o=>o.id===vid))]));
 }
}
const persist=()=>{try{localStorage.setItem(STORE_KEY,JSON.stringify({reviews,drafts,choices,skipped,seedVersion:SEED_VERSION}));}catch(e){alert('Seu navegador não conseguiu salvar. Exporte o JSON antes de sair.')}};
function currentOption(ex){
 let i=Number(choices[ex.id]);if(!Number.isInteger(i)||i<0||i>=ex.options.length)i=0;
 if(reviews[ex.id]?.decision==='approved'){
  const n=ex.options.findIndex(o=>o.id===reviews[ex.id].youtube_id);if(n>=0)i=n;
 }
 return {index:i,option:ex.options[i]};
}
function activeVideo(ex){
 const {option}=currentOption(ex);
 return {...ex,youtubeId:option.id,youtubeTitle:option.title,channel:option.channel,duration:option.duration};
}
function clipDraft(ex){const v=activeVideo(ex);return drafts[ex.id]?.[v.youtubeId]||{};}
function saveDraft(ex,clip){const v=activeVideo(ex);drafts[ex.id]??={};drafts[ex.id][v.youtubeId]=clip;persist();}
const status=id=>reviews[id]?.decision||'pending';
const count=state=>ITEMS.filter(v=>status(v.id)===state).length;
const idx=id=>ITEMS.findIndex(v=>v.id===id);
let currentId=ITEMS[0].id;
let currentFilter='pending';let term='';let clipPreviewedFor='';
function filterItems(){return ITEMS.filter(ex=>{
 const v=activeVideo(ex);
 if(term && ![v.pt,v.en,v.id,v.youtubeTitle].some(x=>String(x).toLocaleLowerCase('pt-BR').includes(term)))return false;
 if(currentFilter==='all')return true;
 if(currentFilter==='long')return v.duration>40;
 return status(ex.id)===currentFilter;
});}
function renderCounts(){
 const a=count('approved'),r=count('rejected');
 $('approvedCount').textContent=a;$('rejectedCount').textContent=r;
 $('pendingCount').textContent=ITEMS.length-a-r;
 $('progressText').textContent=`${a+r} / ${ITEMS.length} revisados`;
 $('progressFill').style.width=`${(a+r)/ITEMS.length*100}%`;
}
function renderList(){
 const list=filterItems();$('visibleCount').textContent=`${list.length} exercícios`;
 if(!list.length){$('list').innerHTML='<p class="empty">Nenhum exercício neste filtro. Escolha “Todos” para rever decisões.</p>';return}
 $('list').innerHTML=list.map(ex=>{
 const v=activeVideo(ex),st=status(ex.id),i=idx(ex.id)+1;
 const dot=st==='approved'?'✓':st==='rejected'?'✕':'○';
 return `<button type="button" class="nav-item ${ex.id===currentId?'current':''}" data-id="${esc(ex.id)}" aria-label="Abrir ${esc(ex.pt)}"><span class="nav-num">${String(i).padStart(2,'0')}</span><span style="min-width:0;flex:1"><span class="nav-title">${esc(ex.pt)}</span><span class="nav-sub">${esc(ex.en)} · ${clock(v.duration)}</span></span><span class="nav-status ${st==='approved'?'ok':st==='rejected'?'no':'wait'}">${dot}</span></button>`;
 }).join('');
 $('list').querySelectorAll('[data-id]').forEach(b=>b.addEventListener('click',()=>select(b.dataset.id)));
}
function stopPlayer(){$('screen').innerHTML=''}
function showPoster(v){
 stopPlayer();const box=document.createElement('div');box.className='poster';
 box.style.setProperty('--thumb',`url("https://i.ytimg.com/vi/${v.youtubeId}/hqdefault.jpg")`);
 const button=document.createElement('button');button.className='play';button.type='button';button.title='Reproduzir vídeo inteiro';button.textContent='▶';button.addEventListener('click',()=>loadPlayer(v,null));
 const msg=document.createElement('span');msg.textContent='Clique para examinar o vídeo';box.append(button,msg);$('screen').append(box);
}
function loadPlayer(v,clip){
 const p=new URLSearchParams({playsinline:'1',controls:'1',rel:'0',autoplay:'1'});
 if(clip){p.set('start',String(clip.start));p.set('end',String(clip.end));clipPreviewedFor=`${v.id}:${v.youtubeId}:${clip.start}:${clip.end}`;}
 const iframe=document.createElement('iframe');iframe.src=`https://www.youtube-nocookie.com/embed/${v.youtubeId}?${p.toString()}`;
 iframe.title='Demonstração de '+v.pt;iframe.allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share';iframe.allowFullscreen=true;iframe.referrerPolicy='strict-origin-when-cross-origin';
 stopPlayer();$('screen').append(iframe);
}
function onClipChange(){
 const ex=byId[currentId],v=activeVideo(ex),clip={start:$('start').value,end:$('end').value};
 saveDraft(ex,clip);clipPreviewedFor='';
 const valid=validClip(v,clip);
 $('clipValidity').textContent=valid?`Trecho selecionado: ${clock(Number(clip.start))}–${clock(Number(clip.end))} (${Number(clip.end)-Number(clip.start)} s). Reveja o trecho antes de aprovar.`:'Informe início e fim válidos, com no máximo 40 segundos.';
 $('clipValidity').style.color=valid?'#7ce7c2':'#ffcf81';$('previewClip').disabled=!valid;
}
function renderAlternatives(ex){
 const v=activeVideo(ex),picker=$('optionPicker');
 $('alternatives').style.display=ex.options.length>1?'block':'none';
 picker.innerHTML=ex.options.map((o,i)=>`<option value="${i}">${i+1} de ${ex.options.length} · ${esc(o.title.slice(0,65))}</option>`).join('');
 picker.value=String(currentOption(ex).index);
 const n=(skipped[ex.id]||[]).length;
 $('optionInfo').textContent=n?`${n} alternativa${n>1?'s':''} rejeitada${n>1?'s':''}`:'';
}
function select(id,shouldScroll=true){
 const ex=byId[id];if(!ex)return;currentId=id;clipPreviewedFor='';
 const v=activeVideo(ex);
 $('counter').textContent=`Exercício ${idx(id)+1} de ${ITEMS.length}`;
 $('namePt').textContent=v.pt;$('nameEn').textContent=v.en;$('description').textContent=v.description;
 $('muscle').textContent=v.muscle||'—';$('duration').textContent=clock(v.duration);$('videoTitle').textContent=v.youtubeTitle;$('channel').textContent=v.channel||'—';
 $('videoLink').href=`https://www.youtube.com/watch?v=${encodeURIComponent(v.youtubeId)}`;
 $('statusChip').className='chip '+(status(id)==='approved'?'ok':status(id)==='rejected'?'no':'');
 $('statusChip').textContent=status(id)==='approved'?'✓ Aprovado':status(id)==='rejected'?'✕ Rejeitado':'Pendente de revisão';
 $('durationChip').textContent=v.duration<=40?'Vídeo inteiro · até 40 s':'Vídeo longo · recorte necessário';$('durationChip').className='chip '+(v.duration>40?'warn':'');
 $('warning').style.display='none';$('warning').textContent='';
 $('clipbox').style.display=v.duration>40?'block':'none';
 $('entire').textContent=v.duration>40?'▶ Assistir ao original para localizar o trecho':'▶ Reproduzir vídeo inteiro';
 if(v.duration>40){const d=clipDraft(ex);$('start').value=d.start??'';$('end').value=d.end??'';$('start').max=String(v.duration);$('end').max=String(v.duration);const valid=validClip(v,d);$('previewClip').disabled=!valid;$('clipValidity').textContent=valid?`Trecho selecionado: ${clock(Number(d.start))}–${clock(Number(d.end))} (${Number(d.end)-Number(d.start)} s).`:'Informe início e fim válidos, com no máximo 40 segundos.';}
 const rev=reviews[id]||{};
 $('reason').value=rev.reason||'';$('observation').value=rev.note||'';
 $('reviewState').textContent=rev.decision?`Última decisão: ${rev.decision==='approved'?'aprovado':'rejeitado'} (${new Date(rev.reviewed_at).toLocaleString('pt-BR')})`:'Ainda não revisado';
 renderAlternatives(ex);showPoster(v);renderCounts();renderList();
 if(shouldScroll)$('maincard').scrollIntoView({behavior:'smooth',block:'start'});
}
function nextUnreviewed(afterId){const start=idx(afterId);for(let n=1;n<=ITEMS.length;n++){const ex=ITEMS[(start+n)%ITEMS.length];if(status(ex.id)==='pending')return ex.id;}return null;}
function decide(decision){
 const ex=byId[currentId],v=activeVideo(ex),clip=clipDraft(ex);
 if(decision==='rejected' && ex.options.length>1){
  const rejected=skipped[ex.id]||[];
  if(!rejected.includes(v.youtubeId))rejected.push(v.youtubeId);
  skipped[ex.id]=rejected;
  // Return to the same exercise with the next alternative, no extra confirmation.
  const next=ex.options.findIndex(o=>!rejected.includes(o.id));
  if(next>=0){choices[ex.id]=next;delete reviews[ex.id];persist();select(ex.id);return;}
 }
 let start_seconds=null,end_seconds=null;
 if(decision==='approved'&&v.duration>40){
  if(!validClip(v,clip)){alert('Para vídeos com mais de 40 segundos, selecione um trecho de até 40 segundos antes de aprovar.');$('start').focus();return;}
  start_seconds=Number(clip.start);end_seconds=Number(clip.end);
  if(clipPreviewedFor!==`${ex.id}:${v.youtubeId}:${start_seconds}:${end_seconds}`){alert('Reproduza o trecho selecionado antes de aprová-lo.');return;}
 }
 reviews[ex.id]={decision,youtube_id:v.youtubeId,start_seconds,end_seconds,reason:decision==='rejected'?$('reason').value:'',note:$('observation').value.trim(),reviewed_at:new Date().toISOString()};
 persist();const next=nextUnreviewed(ex.id);
 if(next)select(next);else {select(ex.id);if(ITEMS.every(x=>status(x.id)!=='pending'))alert('Os 130 exercícios foram revisados! Exporte o JSON das decisões.');}
}
function navigate(dir){let n=idx(currentId);n=(n+dir+ITEMS.length)%ITEMS.length;select(ITEMS[n].id);}
function exportFile(format){
 const results=ITEMS.filter(ex=>status(ex.id)!=='pending').map(ex=>{
  const v=activeVideo(ex);return {exercise_id:ex.id,exercise_name_pt:ex.pt,exercise_name_en:ex.en,youtube_id:reviews[ex.id].youtube_id,duration_seconds:(ex.options.find(o=>o.id===reviews[ex.id].youtube_id)||v).duration,...reviews[ex.id]};
 });
 const summary={approved:count('approved'),rejected:count('rejected'),pending:ITEMS.length-results.length,total:ITEMS.length};
 let payload,mime,filename;
 if(format==='csv'){
  const cols=['exercise_id','exercise_name_pt','exercise_name_en','youtube_id','duration_seconds','decision','start_seconds','end_seconds','reason','note','reviewed_at'];
  const quote=x=>`"${String(x==null?'':x).replace(/"/g,'""')}"`;
  payload='\ufeff'+[cols.join(';'),...results.map(r=>cols.map(k=>quote(r[k])).join(';'))].join('\r\n');mime='text/csv;charset=utf-8';filename='revisao_130_videos_meutreino.csv';
 }else{
  payload=JSON.stringify({schema_version:2,project:'Meu Treino v0.4.6',source:'Pesquisa-videos-130-MeuTreino',exported_at:new Date().toISOString(),summary,results,drafts,choices,skipped},null,2);mime='application/json';filename='revisao_130_videos_meutreino.json';
 }
 const a=document.createElement('a'),url=URL.createObjectURL(new Blob([payload],{type:mime}));a.href=url;a.download=filename;document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),600);
}
function importData(file){
 if(!file)return;const reader=new FileReader();reader.onload=()=>{try{
  const obj=JSON.parse(reader.result);if(!Array.isArray(obj.results))throw Error('JSON não contém resultados');
  const incoming={};const newChoices={};
  for(const r of obj.results){
   const ex=byId[r.exercise_id];if(!ex||!['approved','rejected'].includes(r.decision))throw Error('Exercício ou decisão inválidos');
   const n=ex.options.findIndex(o=>o.id===r.youtube_id);if(n<0)throw Error('Vídeo divergente: '+ex.pt);
   const v={duration:ex.options[n].duration};
   if(r.decision==='approved' && v.duration>40 && !validClip(v,{start:r.start_seconds,end:r.end_seconds}))throw Error('Trecho inválido: '+ex.pt);
   incoming[ex.id]={decision:r.decision,youtube_id:r.youtube_id,start_seconds:r.start_seconds??null,end_seconds:r.end_seconds??null,reason:r.reason||'',note:r.note||'',reviewed_at:r.reviewed_at||new Date().toISOString()};
   newChoices[ex.id]=n;
  }
  if(!confirm(`Importar ${Object.keys(incoming).length} decisões? As decisões dos mesmos exercícios serão substituídas.`))return;
  reviews={...reviews,...incoming};choices={...choices,...newChoices};
  for(const r of obj.results)if(r.start_seconds!=null&&r.end_seconds!=null){drafts[r.exercise_id]??={};drafts[r.exercise_id][r.youtube_id]={start:String(r.start_seconds),end:String(r.end_seconds)};}
  if(obj.skipped&&typeof obj.skipped==='object')skipped={...skipped,...obj.skipped};
  persist();select(currentId);
 }catch(err){alert('Não foi possível importar: '+err.message)}};reader.readAsText(file);
}
$('optionPicker').addEventListener('change',e=>{
 const ex=byId[currentId];choices[ex.id]=Number(e.target.value);
 if(reviews[ex.id]?.decision==='approved')delete reviews[ex.id];
 persist();select(ex.id,false);
});
$('search').addEventListener('input',e=>{term=e.target.value.toLocaleLowerCase('pt-BR').trim();renderList()});
document.querySelectorAll('[data-filter]').forEach(b=>b.addEventListener('click',()=>{currentFilter=b.dataset.filter;document.querySelectorAll('[data-filter]').forEach(x=>x.classList.toggle('active',x===b));renderList()}));
$('entire').addEventListener('click',()=>loadPlayer(activeVideo(byId[currentId]),null));
$('previewClip').addEventListener('click',()=>{const ex=byId[currentId],v=activeVideo(ex),d=clipDraft(ex);if(validClip(v,d))loadPlayer(v,{start:Number(d.start),end:Number(d.end)})});
$('start').addEventListener('input',onClipChange);$('end').addEventListener('input',onClipChange);
$('approve').addEventListener('click',()=>decide('approved'));$('reject').addEventListener('click',()=>decide('rejected'));
$('clear').addEventListener('click',()=>{if(!reviews[currentId] && !skipped[currentId])return;if(!confirm('Remover a decisão deste vídeo e deixá-lo pendente?'))return;delete reviews[currentId];delete skipped[currentId];persist();select(currentId)});
$('prev').addEventListener('click',()=>navigate(-1));$('next').addEventListener('click',()=>navigate(1));
$('exportJson').addEventListener('click',()=>exportFile('json'));$('exportCsv').addEventListener('click',()=>exportFile('csv'));
$('importBtn').addEventListener('click',()=>$('importFile').click());$('importFile').addEventListener('change',e=>{importData(e.target.files?.[0]);e.target.value=''});
document.addEventListener('keydown',e=>{if(['INPUT','TEXTAREA','SELECT'].includes(document.activeElement.tagName))return;if(e.key==='ArrowRight')navigate(1);if(e.key==='ArrowLeft')navigate(-1)});
persist();const first=ITEMS.find(ex=>status(ex.id)==='pending')||ITEMS[0];select(first.id,false);
})();