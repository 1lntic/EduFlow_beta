(function(c){
  'use strict';
  if(!/^ef_[a-f0-9]{32}$/.test(c.job))return 'invalid';
  const state={result:{status:'pending'},cancel:null};window[c.job]=state;
  const controller=new AbortController();state.cancel=()=>controller.abort();
  const timer=setTimeout(()=>controller.abort(),25000);
  const text=(v,max=600)=>{
    if(v==null)return '';
    if(typeof v!=='string'&&typeof v!=='number')throw new Error('SCHEMA_FIELD');
    return String(v).replace(/<[^>]*>/g,' ').replace(/\s+/g,' ').trim().slice(0,max);
  };
  const key=v=>{if(typeof v!=='string'||!v||v.length>500)throw new Error('SCHEMA_MESSAGE_ID');return v;};
  async function request(path){
    const r=await fetch(c.base+'/api/'+path,{method:'GET',credentials:'same-origin',redirect:'error',cache:'no-store',headers:{Accept:'application/json'},signal:controller.signal});
    if(!r.ok)throw new Error('HTTP_'+r.status);
    if(!(r.headers.get('content-type')||'').includes('json'))throw new Error('LOGIN_OR_SCHEMA');
    const raw=await r.text();if(raw.length>2097152)throw new Error('PAYLOAD_TOO_LARGE');
    try{return JSON.parse(raw);}catch(e){throw new Error('INVALID_JSON');}
  }
  async function run(){
    const base=new URL(c.base);
    if(location.protocol!=='https:'||location.hostname!=='wiadomosci.eduvulcan.pl'||base.origin!==location.origin||!/^\/[a-zA-Z0-9_-]+$/.test(base.pathname)||!(location.pathname===base.pathname||location.pathname.startsWith(base.pathname+'/')))throw new Error('WRONG_PORTAL');
    if(c.operation==='inbox'){
      const items=await request('Odebrane?idLastWiadomosc=0&pageSize=50');
      if(!Array.isArray(items)||items.length>200)throw new Error('SCHEMA_INBOX');
      const messages=items.map(m=>{
        if(!m||typeof m!=='object'||Array.isArray(m)||typeof m.przeczytana!=='boolean')throw new Error('SCHEMA_MESSAGE');
        return {id:key(m.apiGlobalKey),subject:text(m.temat),sender:text(m.korespondenci),date:text(m.data),mailbox:text(m.skrzynka),unread:!m.przeczytana,hasAttachments:m.hasZalaczniki===true};
      });
      return {messages,fetchedAt:Date.now(),scope:'account-inbox',limit:50};
    }
    if(c.operation==='read'){
      const id=key(c.id);
      const detail=await request('WiadomoscSzczegoly?apiGlobalKey='+encodeURIComponent(id));
      if(!detail||typeof detail!=='object'||Array.isArray(detail)||typeof detail.tresc!=='string')throw new Error('SCHEMA_BODY');
      const withoutActive=detail.tresc.replace(/<(script|style)\b[^>]*>[\s\S]*?<\/\1\s*>/gi,'');
      return {id,body:text(withoutActive,50000),fetchedAt:Date.now()};
    }
    throw new Error('INVALID_OPERATION');
  }
  run().then(data=>{if(window[c.job]===state)state.result={status:'done',data};}).catch(e=>{
    const m=String(e&&e.message||'NETWORK_ERROR');
    state.result={status:'error',error:/^(HTTP_\d+|SCHEMA_[A-Z_]+|LOGIN_OR_SCHEMA|PAYLOAD_TOO_LARGE|INVALID_JSON|WRONG_PORTAL|INVALID_OPERATION)$/.test(m)?m:(controller.signal.aborted?'TIMEOUT':'NETWORK_ERROR')};
  }).finally(()=>clearTimeout(timer));
  return 'started';
})(__CONFIG__);
