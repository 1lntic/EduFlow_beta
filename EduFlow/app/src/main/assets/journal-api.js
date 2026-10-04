(function(c){
  'use strict';
  if(!/^ef_[a-f0-9]{32}$/.test(c.job))return 'invalid';
  const job={result:{status:'pending'},cancel:null};
  window[c.job]=job;
  const abort=new AbortController();
  job.cancel=()=>abort.abort();
  const timeout=setTimeout(()=>abort.abort(),30000);
  const text=(v,max=400)=>{
    if(v===null||v===undefined)return '';
    if(typeof v!=='string'&&typeof v!=='number')throw new Error('SCHEMA_FIELD');
    return String(v).replace(/<[^>]*>/g,' ').replace(/\s+/g,' ').trim().slice(0,max);
  };
  const describe=v=>Array.isArray(v)?'array':(v&&typeof v==='object'?'object:'+Object.keys(v).slice(0,12).map(k=>k.replace(/[^a-zA-Z0-9_]/g,'').slice(0,35)).join(','):typeof v);
  async function get(path){
    const r=await fetch(c.base+'/api/'+path,{method:'GET',credentials:'same-origin',redirect:'error',cache:'no-store',headers:{Accept:'application/json'},signal:abort.signal});
    if(!r.ok)throw new Error('HTTP_'+r.status);
    const ct=r.headers.get('content-type')||'';
    if(!ct.includes('json'))throw new Error('NOT_JSON_LOGIN_OR_SCHEMA');
    const body=await r.text();
    if(body.length>1048576)throw new Error('PAYLOAD_TOO_LARGE');
    try{return JSON.parse(body);}catch(e){throw new Error('INVALID_JSON');}
  }
  function list(v,label){
    if(!Array.isArray(v))throw new Error('SCHEMA_'+label+' '+describe(v));
    if(v.length>2000)throw new Error('TOO_MANY_'+label);
    return v;
  }
  async function run(){
    const base=new URL(c.base);
    if(location.protocol!=='https:'||location.hostname!=='uczen.eduvulcan.pl'||base.origin!==location.origin||!/^\/[a-zA-Z0-9_-]+$/.test(base.pathname)||!(location.pathname===base.pathname||location.pathname.startsWith(base.pathname+'/')))throw new Error('WRONG_PORTAL');
    const context=await get('Context');
    if(!context||!Array.isArray(context.uczniowie))throw new Error('SCHEMA_CONTEXT '+describe(context));
    const students=context.uczniowie.filter(s=>s&&s.aktywny===true&&typeof s.key==='string'&&s.key.length>0&&s.key.length<500).map(s=>({key:s.key,name:text(s.uczen),className:text(s.oddzial),school:text(s.jednostka),journalId:text(s.idDziennik)}));
    if(students.length===0)throw new Error('NO_ACTIVE_STUDENT');
    if(students.length>32)throw new Error('TOO_MANY_STUDENTS');
    if(c.operation==='context')return {students};
    if(c.operation!=='student')throw new Error('INVALID_OPERATION');
    const student=students.find(s=>s.key===c.studentKey&&s.journalId===c.journalId);
    if(!student)throw new Error('STUDENT_CONTEXT_CHANGED');
    const key=encodeURIComponent(student.key);
    const [g,p]=await Promise.all([
      get('OcenyTablica?key='+key),
      get('PlanZajec?key='+key+'&dataOd='+encodeURIComponent(c.from)+'&dataDo='+encodeURIComponent(c.to)+'&zakresDanych=2')
    ]);
    const grades=list(g,'GRADES').map(v=>{
      if(!v||typeof v!=='object'||Array.isArray(v))throw new Error('SCHEMA_GRADES_ITEM');
      const subject=text(v.przedmiot),grade=text(v.ocena,40);
      if(!subject||!grade)throw new Error('SCHEMA_GRADES_ITEM '+describe(v));
      const weight=typeof v.waga==='number'&&Number.isFinite(v.waga)&&v.waga>=0?v.waga:null;
      return {subject,grade,date:text(v.data),category:text(v.kategoria),comment:text(v.komentarz,800),weight};
    });
    const lessons=list(p,'PLAN').map(v=>{
      if(!v||typeof v!=='object'||Array.isArray(v))throw new Error('SCHEMA_PLAN_ITEM');
      const subject=text(v.przedmiot);
      if(!subject)throw new Error('SCHEMA_PLAN_ITEM '+describe(v));
      return {subject,startsAt:text(v.godzinaOd),endsAt:text(v.godzinaDo),date:text(v.data)||c.date,teacher:text(v.prowadzacy),room:text(v.sala)};
    });
    return {name:student.name,className:student.className,school:student.school,grades,lessons,date:c.date,fetchedAt:Date.now(),gradeScope:'OcenyTablica'};
  }
  run().then(data=>{if(window[c.job]===job)job.result={status:'done',data};}).catch(e=>{
    const message=String(e&&e.message||'NETWORK_ERROR');
    const allowed=/^(HTTP_\d+|SCHEMA_[A-Z_]+(?: [a-zA-Z0-9_:,]+)?|NOT_JSON_LOGIN_OR_SCHEMA|INVALID_JSON|PAYLOAD_TOO_LARGE|TOO_MANY_[A-Z_]+|WRONG_PORTAL|NO_ACTIVE_STUDENT|INVALID_OPERATION|STUDENT_CONTEXT_CHANGED)$/;
    job.result={status:'error',error:allowed.test(message)?message:(abort.signal.aborted?'TIMEOUT':'NETWORK_ERROR')};
  }).finally(()=>clearTimeout(timeout));
  return 'started';
})(__CONFIG__);
