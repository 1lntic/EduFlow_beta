(function(c){
  'use strict';
  if(!/^ef_[a-f0-9]{32}$/.test(c.job))return 'invalid';
  const task={result:{status:'pending'},cancel:null};window[c.job]=task;
  const ac=new AbortController();task.cancel=()=>ac.abort();const timer=setTimeout(()=>ac.abort(),30000);
  const definitions={
    achievements:{endpoint:'Osiagniecia',scope:'Osiągnięcia zwrócone przez API'},
    remarks:{endpoint:'Uwagi',scope:'Pochwały i uwagi zwrócone przez API'},
    meetings:{endpoint:'Zebrania',scope:'Zebrania dostępne dla profilu'},
    homework:{endpoint:'SprawdzianyZadaniaDomowe',scope:'Sprawdziany i zadania w podanym zakresie dat'},
    attendance:{endpoint:'FrekwencjaStatystyki',scope:'Statystyki frekwencji; bez historii pojedynczych nieobecności'}
  };
  const secret=/^(id|key|apiGlobalKey|token|cookie|password|haslo|hasło|secret|pesel|login|url|href|link)/i;
  const label=s=>String(s).replace(/([a-ząćęłńóśźż])([A-ZĄĆĘŁŃÓŚŹŻ])/g,'$1 $2').replace(/[_-]/g,' ').slice(0,140);
  function plain(v){
    if(typeof v!=='string'&&typeof v!=='number'&&typeof v!=='boolean')throw new Error('SCHEMA_VALUE');
    const value=String(v).replace(/<(script|style)\b[^>]*>[\s\S]*?<\/\1\s*>/gi,'').replace(/<[^>]*>/g,' ').replace(/\s+/g,' ').trim();
    if(value.length>5000)throw new Error('LIMIT_TEXT');return value;
  }
  function flatten(v,path,out,depth){
    if(v==null)return;
    if(depth>5)throw new Error('LIMIT_DEPTH');
    if(Array.isArray(v)){
      if(v.length>80)throw new Error('LIMIT_NESTED_LIST');
      v.forEach((x,i)=>flatten(x,path+' '+(i+1),out,depth+1));return;
    }
    if(typeof v==='object'){
      const keys=Object.keys(v);if(keys.length>100)throw new Error('LIMIT_KEYS');
      keys.filter(k=>!secret.test(k)).forEach(k=>flatten(v[k],path?path+' / '+label(k):label(k),out,depth+1));return;
    }
    if(out.length>=200)throw new Error('LIMIT_FIELDS');
    out.push({label:path||'Wartość',value:plain(v)});
  }
  function normalize(raw){
    if(!raw||typeof raw!=='object')throw new Error('SCHEMA_MODULE');
    if(!Array.isArray(raw)&&(raw.error||raw.success===false||raw.status===false))throw new Error('API_REJECTED');
    const list=Array.isArray(raw)?raw:[raw];
    if(list.length>1000)throw new Error('LIMIT_RECORDS');
    return list.map((row,i)=>{
      if(!row||typeof row!=='object'||Array.isArray(row))throw new Error('SCHEMA_RECORD');
      const fields=[];flatten(row,'',fields,0);if(!fields.length)throw new Error('SCHEMA_EMPTY_RECORD');
      const candidate=[row.tytul,row.temat,row.nazwa,row.przedmiotNazwa].find(x=>typeof x==='string'&&x.trim());
      return {title:candidate?plain(candidate):'Pozycja '+(i+1),fields};
    });
  }
  async function get(path,query){
    const r=await fetch(c.base+'/api/'+path+(query?'?'+query:''),{method:'GET',credentials:'same-origin',redirect:'error',cache:'no-store',headers:{Accept:'application/json'},signal:ac.signal});
    if(!r.ok)throw new Error('HTTP_'+r.status);
    if(!(r.headers.get('content-type')||'').includes('json'))throw new Error('LOGIN_OR_SCHEMA');
    const body=await r.text();if(body.length>2097152)throw new Error('LIMIT_PAYLOAD');
    try{return JSON.parse(body);}catch(e){throw new Error('INVALID_JSON');}
  }
  async function run(){
    const b=new URL(c.base);
    if(location.protocol!=='https:'||location.hostname!=='uczen.eduvulcan.pl'||b.origin!==location.origin||!/^\/[a-zA-Z0-9_-]+$/.test(b.pathname)||!(location.pathname===b.pathname||location.pathname.startsWith(b.pathname+'/')))throw new Error('WRONG_PORTAL');
    if(c.operation!=='section'||!Object.prototype.hasOwnProperty.call(definitions,c.name))throw new Error('INVALID_SECTION');
    const context=await get('Context','');
    if(!context||!Array.isArray(context.uczniowie))throw new Error('SCHEMA_CONTEXT');
    const student=context.uczniowie.find(s=>s&&s.aktywny===true&&typeof s.key==='string'&&s.key===c.studentKey&&String(s.idDziennik??'')===c.journalId);
    if(!student)throw new Error('STUDENT_CONTEXT_CHANGED');
    const query=new URLSearchParams({key:student.key});
    if(c.name==='attendance')query.set('idPrzedmiot','-1');
    if(c.name==='homework'){
      if(typeof c.from!=='string'||typeof c.to!=='string')throw new Error('INVALID_RANGE');
      const from=Date.parse(c.from),to=Date.parse(c.to);
      if(!Number.isFinite(from)||!Number.isFinite(to)||to<from||to-from>366*86400000)throw new Error('INVALID_RANGE');
      query.set('dataOd',c.from);query.set('dataDo',c.to);
    }
    const definition=definitions[c.name];const raw=await get(definition.endpoint,query.toString());
    const records=normalize(raw);
    return {module:c.name,records,total:records.length,scope:definition.scope,fetchedAt:Date.now(),from:c.name==='homework'?c.from:null,to:c.name==='homework'?c.to:null};
  }
  run().then(data=>{if(window[c.job]===task)task.result={status:'done',data};}).catch(e=>{
    const code=String(e&&e.message||'NETWORK_ERROR');
    task.result={status:'error',error:/^(HTTP_\d+|SCHEMA_[A-Z_]+|LIMIT_[A-Z_]+|API_REJECTED|LOGIN_OR_SCHEMA|INVALID_JSON|WRONG_PORTAL|INVALID_SECTION|STUDENT_CONTEXT_CHANGED|INVALID_RANGE)$/.test(code)?code:(ac.signal.aborted?'TIMEOUT':'NETWORK_ERROR')};
  }).finally(()=>clearTimeout(timer));return 'started';
})(__CONFIG__);
