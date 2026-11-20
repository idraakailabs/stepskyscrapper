(function(){
var C=0.866, W=36, LOB=13, FH=8;
var SK={dawn:['#F2B9A0',0.12],dusk:['#4E3F7A',0.3],night:['#121A33',0.58]};
var LIT={night:0.62,dusk:0.38};
var AMBER='#E8A23A',AMBER_D='#7A5212',PLANT='#7EA26A',PLANT2='#5E8A50',TRUNK='#7A5C3E',LITC='#FFD98A';
var ST={
 deco:{L:'#E9DCC3',R:'#CBB793',T:'#F4ECDD',win:'#5B503F',trim:'#B8873A',lobbyL:'#DCC9A6',lobbyR:'#BCA67F',setbacks:[[0.55,0.8],[0.82,0.58]]},
 glass:{L:'#A8BFCB',R:'#7D98A8',T:'#D3DFE5',win:'#4D6A7D',mull:'#C8D7DF',lobbyL:'#90A8B5',lobbyR:'#6E8899',setbacks:[[0.72,0.86]]},
 brick:{L:'#C57B5D',R:'#A26048',T:'#DDB09A',win:'#3E3430',sill:'#F1E7D9',lobbyL:'#B46B50',lobbyR:'#925540',setbacks:[[0.78,0.84]]},
 brut:{L:'#C6C1B8',R:'#A5A097',T:'#D8D4CC',win:'#47433E',slab:'#DAD6CE',lobbyL:'#B4AFA6',lobbyR:'#948F86',setbacks:[]},
 eco:{L:'#EEEEE4',R:'#CED1C4',T:'#F5F5ED',win:'#5C7268',plant:'#7EA26A',lobbyL:'#DCDED1',lobbyR:'#BFC2B4',setbacks:[[0.62,0.84]]}
};
var GR={plaza:{T:'#E2DACD',L:'#CEC4B4',R:'#B7AC9A'},park:{T:'#C9D5B1',L:'#BDB3A1',R:'#A39987'}};
var KF='@keyframes ssDrop{0%{transform:translateY(-26px);opacity:0}65%{transform:translateY(1.6px);opacity:1}100%{transform:translateY(0)}}@keyframes ssLift{0%{transform:translateY(-44px)}78%{transform:translateY(1.4px)}100%{transform:translateY(0)}}@keyframes ssFlick{0%{opacity:.1}35%{opacity:1}45%{opacity:.35}60%,100%{opacity:1}}@media (prefers-reduced-motion:reduce){*{animation:none!important}}';
function getR(){return (typeof React!=='undefined'&&React)||window.React;}
function hx(h){h=h.replace('#','');return [parseInt(h.substr(0,2),16),parseInt(h.substr(2,2),16),parseInt(h.substr(4,2),16)];}
function mix(a,b,t){var A=hx(a),B=hx(b);return '#'+A.map(function(v,i){var r=Math.round(v+(B[i]-v)*t);return ('0'+r.toString(16)).slice(-2);}).join('');}
function hash(a,b,c){var x=Math.sin(a*127.1+b*311.7+c*74.7)*43758.5453;return x-Math.floor(x);}
function num(v,d){var n=parseFloat(v);return isNaN(n)?d:n;}
function Ctx(R,time,anim,seed){this.R=R;this.k=0;this.minX=1e9;this.maxX=-1e9;this.minY=1e9;this.maxY=-1e9;this.time=time||'day';this.anim=anim||'';this.seed=seed||0;}
Ctx.prototype.pt=function(x,y,z){var X=(x-y)*C,Y=(x+y)*0.5-z;if(X<this.minX)this.minX=X;if(X>this.maxX)this.maxX=X;if(Y<this.minY)this.minY=Y;if(Y>this.maxY)this.maxY=Y;return [X,Y];};
Ctx.prototype.p=function(x,y,z){var a=this.pt(x,y,z);return a[0].toFixed(1)+','+a[1].toFixed(1);};
Ctx.prototype.tint=function(c){if(this.mono){var A=hx(c),l=(A[0]*.3+A[1]*.59+A[2]*.11)/255;return l>.5?mix(this.mono,'#ffffff',(l-.5)*.5):mix(this.mono,'#000000',(.5-l)*.5);}var s=SK[this.time];return s?mix(c,s[0],s[1]):c;};
Ctx.prototype.poly=function(list,pts,fill,ex){var o={key:this.k++,points:pts.join(' '),fill:fill};if(ex)for(var q in ex)o[q]=ex[q];list.push(this.R.createElement('polygon',o));};
Ctx.prototype.box=function(list,x,y,z,w,d,h,L,R,T){var p=this.p.bind(this);
 this.poly(list,[p(x,y+d,z),p(x+w,y+d,z),p(x+w,y+d,z+h),p(x,y+d,z+h)],this.tint(L));
 this.poly(list,[p(x+w,y+d,z),p(x+w,y,z),p(x+w,y,z+h),p(x+w,y+d,z+h)],this.tint(R));
 if(T)this.poly(list,[p(x,y,z+h),p(x+w,y,z+h),p(x+w,y+d,z+h),p(x,y+d,z+h)],this.tint(T));};
Ctx.prototype.line=function(list,a,b,col,w,ex){var o={key:this.k++,x1:a[0],y1:a[1],x2:b[0],y2:b[1],stroke:col,strokeWidth:w,strokeLinecap:'round'};if(ex)for(var q in ex)o[q]=ex[q];list.push(this.R.createElement('line',o));};
Ctx.prototype.circ=function(list,c,r,fill){list.push(this.R.createElement('circle',{key:this.k++,cx:c[0],cy:c[1],r:r,fill:fill}));};
Ctx.prototype.g=function(list,ch,style){list.push(this.R.createElement('g',{key:this.k++,style:style||undefined},ch));};
function pat(st,s,fw,fh){var r=[],n,p,i,c,h;
 if(s==='deco'){n=Math.max(3,Math.round(fw/8));p=fw/n;for(i=0;i<n;i++){c=(i+.5)*p;h=p*.2;r.push([c-h,c+h,1.6,fh-1.4,st.win,'w']);}}
 else if(s==='glass'){r.push([.6,fw-.6,1,fh-.5,st.win,'w']);for(c=5.6;c<fw-2;c+=5)r.push([c-.25,c+.25,1,fh-.5,st.mull,'m']);}
 else if(s==='brick'){n=Math.max(2,Math.round(fw/11));p=fw/n;for(i=0;i<n;i++){c=(i+.5)*p;h=p*.22;r.push([c-h,c+h,2.2,fh-1.8,st.win,'w']);r.push([c-h-.6,c+h+.6,1.4,2.2,st.sill,'m']);}}
 else if(s==='brut'){r.push([0,fw,0,1.6,st.slab,'m']);n=Math.max(2,Math.round(fw/15));p=fw/n;for(i=0;i<n;i++){c=(i+.5)*p;h=p*.36;r.push([c-h,c+h,2.6,fh-1.6,st.win,'w']);}}
 else {r.push([.8,fw-.8,2.4,fh-.7,st.win,'w']);r.push([0,fw,0,1.5,st.plant,'m']);r.push([0,fw,1.2,1.8,PLANT2,'m']);}
 return r;}
function lobbyPat(st,fw,side){var b=st.trim||st.T;
 if(side===0)return [[fw/2-4,fw/2+4,0,8.5,st.win,'w'],[3,fw/2-6.5,3,10,st.win,'w'],[fw/2+6.5,fw-3,3,10,st.win,'w'],[0,fw,11.4,13,b,'m']];
 return [[3,11,3,10,st.win,'w'],[14,22,3,10,st.win,'w'],[25,33,3,10,st.win,'w'],[0,fw,11.4,13,b,'m']];}
function faces(ctx,list,st,sname,x,y,z,w,d,fh,fi,lobby){var p=ctx.p.bind(ctx),acc={};
 [0,1].forEach(function(side){var fw=side?d:w,rs=lobby?lobbyPat(st,fw,side):pat(st,sname,fw,fh);
  rs.forEach(function(r,ri){var col=r[4],ex=null;
   if(r[5]==='w'){var lr=LIT[ctx.time]||0,hv=hash(fi+ctx.seed*31,side,ri);
    if(lr&&hv<lr){col=LITC;if(ctx.anim.indexOf('lights')>=0)ex={style:{animation:'ssFlick 900ms steps(1,end) both',animationDelay:Math.round(hash(ri,fi,side+7)*1400)+'ms'}};}
    else col=ctx.tint(side?mix(col,'#000000',.12):col);}
   else col=ctx.tint(side?mix(col,'#000000',.1):col);
   var pts=side===0?[p(x+r[0],y+d,z+r[2]),p(x+r[1],y+d,z+r[2]),p(x+r[1],y+d,z+r[3]),p(x+r[0],y+d,z+r[3])]:[p(x+w,y+d-r[0],z+r[2]),p(x+w,y+d-r[1],z+r[2]),p(x+w,y+d-r[1],z+r[3]),p(x+w,y+d-r[0],z+r[3])];
   if(ex)ctx.poly(list,pts,col,ex);else acc[col]=(acc[col]||'')+'M'+pts.join('L')+'Z';});});
 for(var c in acc)list.push(ctx.R.createElement('path',{key:ctx.k++,d:acc[c],fill:c}));}
function tree(ctx,list,x,y,z,r){ctx.line(list,ctx.pt(x,y,z),ctx.pt(x,y,z+r*1.3),ctx.tint(TRUNK),1.1);ctx.circ(list,ctx.pt(x,y,z+r*1.7),r,ctx.tint(PLANT));ctx.circ(list,ctx.pt(x+r*.3,y-r*.3,z+r*2.1),r*.55,ctx.tint('#94B57F'));}
function crown(ctx,list,st,s,x,y,z,w){var cx=x+w/2,cy=y+w/2,b=ctx.box.bind(ctx);
 if(s==='deco'){b(list,x+w*.12,y+w*.12,z,w*.76,w*.76,5,st.L,st.R,st.T);b(list,x+w*.27,y+w*.27,z+5,w*.46,w*.46,6,st.L,st.R,st.T);b(list,x+w*.39,y+w*.39,z+11,w*.22,w*.22,7,mix(st.trim,'#ffffff',.25),st.trim,mix(st.trim,'#ffffff',.45));ctx.line(list,ctx.pt(cx,cy,z+18),ctx.pt(cx,cy,z+34),ctx.tint(st.trim),1.3);ctx.circ(list,ctx.pt(cx,cy,z+34.5),1.6,AMBER);return 36;}
 if(s==='glass'){b(list,x+w*.06,y+w*.06,z,w*.88,w*.88,3,st.R,mix(st.R,'#000000',.12),st.T);b(list,x+w*.22,y+w*.22,z+3,w*.56,w*.56,6,st.L,st.R,st.T);ctx.line(list,ctx.pt(cx,cy,z+9),ctx.pt(cx,cy,z+28),ctx.tint('#5E6F78'),1);ctx.circ(list,ctx.pt(cx,cy,z+28.5),1.4,AMBER);return 30;}
 if(s==='brick'){b(list,x-1.5,y-1.5,z,w+3,w+3,2.4,mix(st.R,'#000000',.05),mix(st.R,'#000000',.2),st.T);var tx=x+w*.5,ty=y+w*.12,q=9,zl=z+2.4;[[0,0],[q,0],[q,q],[0,q]].forEach(function(o){ctx.line(list,ctx.pt(tx+o[0],ty+o[1],zl),ctx.pt(tx+o[0],ty+o[1],zl+5),ctx.tint('#5A4636'),.9);});b(list,tx,ty,zl+5,q,q,6,'#8C6A52','#71553F','#9C7A60');var ap=ctx.pt(tx+q/2,ty+q/2,zl+15),p=ctx.p.bind(ctx),aps=ap[0].toFixed(1)+','+ap[1].toFixed(1);ctx.poly(list,[p(tx,ty+q,zl+11),p(tx+q,ty+q,zl+11),aps],ctx.tint('#6B5242'));ctx.poly(list,[p(tx+q,ty+q,zl+11),p(tx+q,ty,zl+11),aps],ctx.tint('#57423A'));return 18;}
 if(s==='brut'){b(list,x-3,y-3,z,w+6,w+6,7,st.L,st.R,st.T);var p2=ctx.p.bind(ctx);ctx.poly(list,[p2(x-1,y+w+3,z+2.2),p2(x+w+1,y+w+3,z+2.2),p2(x+w+1,y+w+3,z+4.8),p2(x-1,y+w+3,z+4.8)],ctx.tint(st.win));ctx.poly(list,[p2(x+w+3,y+w+1,z+2.2),p2(x+w+3,y-1,z+2.2),p2(x+w+3,y-1,z+4.8),p2(x+w+3,y+w+1,z+4.8)],ctx.tint(mix(st.win,'#000000',.15)));b(list,x+w*.3,y+w*.3,z+7,w*.4,w*.4,4,st.lobbyL,st.lobbyR,st.T);return 11;}
 b(list,x,y,z,w,w,1.6,'#8FA77A','#748C62',PLANT);tree(ctx,list,x+w*.28,y+w*.25,z+1.6,4);tree(ctx,list,x+w*.75,y+w*.3,z+1.6,3.4);tree(ctx,list,x+w*.35,y+w*.72,z+1.6,3.6);tree(ctx,list,x+w*.74,y+w*.74,z+1.6,2.8);return 12;}
function drawTower(ctx,list,o){var st=ST[o.style]||ST.deco,sn=ST[o.style]?o.style:'deco',ox=o.ox||0,oy=o.oy||0;
 var target=Math.max(o.target||o.floors||1,1),floors=Math.max(0,Math.min(o.floors,target)),drop=ctx.anim.indexOf('drop')>=0;
 function sc(i){var s=1;st.setbacks.forEach(function(b){if(i>=Math.round(b[0]*target))s=b[1];});return s;}
 var lob=[];ctx.box(lob,ox,oy,0,W,W,LOB,st.lobbyL,st.lobbyR,st.T);faces(ctx,lob,st,sn,ox,oy,0,W,W,LOB,-1,true);ctx.g(list,lob,drop?{animation:'ssDrop 420ms cubic-bezier(.3,1.3,.5,1) both'}:null);
 for(var i=0;i<floors;i++){var w=W*sc(i),of=(W-w)/2,z=LOB+i*FH,fl=[];ctx.box(fl,ox+of,oy+of,z,w,w,FH,st.L,st.R,st.T);faces(ctx,fl,st,sn,ox+of,oy+of,z,w,w,FH,i,false);ctx.g(list,fl,drop?{animation:'ssDrop 420ms cubic-bezier(.3,1.3,.5,1) both',animationDelay:((i+1)*140)+'ms'}:null);}
 var topZ=LOB+floors*FH,lw=W*sc(Math.max(floors-1,0)),lo=(W-lw)/2,hookZ=topZ+FH+6,p=ctx.p.bind(ctx);
 if(floors<target){var gc=(ctx.time==='night'||ctx.time==='dusk')?'#C9C2E0':'#6A635A',start=floors;
  if(o.progress>0){var w2=W*sc(floors),o2=(W-w2)/2,x=ox+o2,y=oy+o2,z2=topZ,ph=FH*o.progress,pr=[];
   ctx.box(pr,x,y,z2,w2,w2,FH,st.L,st.R,null);pr.forEach(function(e){});var tr=[];ctx.box(tr,x,y,z2,w2,w2,FH,st.L,st.R,null);
   list.push(ctx.R.createElement('g',{key:ctx.k++,opacity:.28},tr));var so=[];ctx.box(so,x,y,z2,w2,w2,ph,st.L,st.R,st.T);ctx.g(list,so);
   ctx.poly(list,[p(x,y+w2,z2),p(x+w2,y+w2,z2),p(x+w2,y+w2,z2+FH),p(x,y+w2,z2+FH)],'none',{stroke:AMBER,strokeWidth:.8,strokeDasharray:'1.6 1.6'});
   ctx.poly(list,[p(x+w2,y+w2,z2),p(x+w2,y,z2),p(x+w2,y,z2+FH),p(x+w2,y+w2,z2+FH)],'none',{stroke:AMBER,strokeWidth:.8,strokeDasharray:'1.6 1.6'});
   start=floors+1;hookZ=topZ+FH+8;}
  var a=start;while(a<target){var s=sc(a),b2=a;while(b2<target&&sc(b2)===s)b2++;var gw=W*s,go=(W-gw)/2,gx=ox+go,gy=oy+go,za=LOB+a*FH,zb=LOB+b2*FH,ge={stroke:gc,strokeWidth:.6,strokeDasharray:'2 2.4',strokeOpacity:.55};
   ctx.poly(list,[p(gx,gy+gw,za),p(gx+gw,gy+gw,za),p(gx+gw,gy+gw,zb),p(gx,gy+gw,zb)],'none',ge);ctx.poly(list,[p(gx+gw,gy+gw,za),p(gx+gw,gy,za),p(gx+gw,gy,zb),p(gx+gw,gy+gw,zb)],'none',ge);
   if(b2===target)ctx.poly(list,[p(gx,gy,zb),p(gx+gw,gy,zb),p(gx+gw,gy+gw,zb),p(gx,gy+gw,zb)],'none',ge);a=b2;}}
 var ch=0;if(floors>=target&&o.crown!=='none'){var cl=[];ch=crown(ctx,cl,st,sn,ox+lo,oy+lo,topZ,lw);ctx.g(list,cl,o.crown==='lift'?{animation:'ssLift 760ms cubic-bezier(.25,1,.4,1) both',animationDelay:'200ms'}:null);hookZ=topZ+ch+2;}
 if(o.crane){var mx=ox+W+10,my=oy+W/2,jz=Math.max(topZ+FH+(ch||0)+20,60),col=ctx.tint(AMBER),dk=ctx.tint(AMBER_D);
  ctx.line(list,ctx.pt(mx,my,0),ctx.pt(mx,my,jz+5),col,1.8);ctx.line(list,ctx.pt(mx+2,my,0),ctx.pt(mx+2,my,jz),col,1.2);
  for(var zz=0;zz<jz;zz+=6)ctx.line(list,ctx.pt(mx,my,zz),ctx.pt(mx+2,my,zz+6),dk,.5);
  var hx2=ox+W/2;ctx.line(list,ctx.pt(mx+14,my,jz),ctx.pt(ox-8,my,jz),col,1.5);ctx.line(list,ctx.pt(mx,my,jz+6),ctx.pt(ox-8,my,jz),dk,.6);ctx.line(list,ctx.pt(mx,my,jz+6),ctx.pt(mx+14,my,jz),dk,.6);
  ctx.box(list,mx+10,my-2,jz-5,4,4,5,'#8A8278','#6E675E','#9E978C');ctx.box(list,mx-3,my-2,jz-4,4,4,4,col,dk,col);
  ctx.line(list,ctx.pt(hx2,my,jz),ctx.pt(hx2,my,hookZ+(o.crown==='lift'?0:4)),dk,.6);
  if(floors<target){ctx.box(list,hx2-7,my-3,hookZ-3,14,6,3,st.L,st.R,st.T);}}
 return topZ+ch;}
function finish(R,ctx,els,props,label,minh){var pad=num(props.pad,6),minX=ctx.minX-pad,maxX=ctx.maxX+pad,minY=ctx.minY-pad,maxY=ctx.maxY+pad;
 if(minh&&maxY-minY<minh)minY=maxY-minh;var minw=num(props.minw,0);if(minw&&maxX-minX<minw){var cx=(minX+maxX)/2;minX=cx-minw/2;maxX=cx+minw/2;}
 return R.createElement('svg',{viewBox:[minX,minY,maxX-minX,maxY-minY].map(function(v){return v.toFixed(1)}).join(' '),width:'100%',height:'100%',preserveAspectRatio:props.align||'xMidYMax meet',role:'img','aria-label':label,style:{display:'block',overflow:'visible'}},[R.createElement('style',{key:'kf'},KF)].concat(els));}
function SSTower(props){var R=getR(),ctx=new Ctx(R,props.time,props.animate,num(props.seed,1)),els=[],g=props.ground||'plaza';if(props.mono)ctx.mono=props.mono;
 var o={style:props.facade||'deco',floors:num(props.floors,10),target:num(props.target,num(props.floors,10)),progress:num(props.progress,0),crown:props.crown||'auto',crane:props.crane===true||props.crane==='true'};
 if(g!=='none'){var gc=GR[g]||GR.plaza;ctx.box(els,-9,-9,-3,W+18,W+18,3,gc.L,gc.R,gc.T);}
 drawTower(ctx,els,o);
 if(g!=='none'){tree(ctx,els,-4,W+4,0,3.2);tree(ctx,els,W+5,-3,0,2.8);if(g==='park')tree(ctx,els,W+5,W+5,0,3);}
 return finish(R,ctx,els,props,props.label||((o.floors)+' of '+o.target+' floors built'),num(props.minh,0));}
function SSSkyline(props){var R=getR(),ctx=new Ctx(R,props.time,props.animate,3),els=[],S=W+16,m=11,gc=GR[props.ground||'park']||GR.park;
 var ts=(props.towers||'deco:24').split(',').map(function(t){var a=t.split(':'),f=(a[1]||'10').split('/');return {style:a[0],floors:num(f[0],10),target:num(f[1],num(f[0],10))};});
 var n=ts.length,L=(n-1)*S,p=ctx.p.bind(ctx),th=4;
 var hex=[[-m,-m],[L-m,-L-m],[L+W+m,-L-m],[L+W+m,-L+W+m],[W+m,W+m],[-m,W+m]];
 ctx.poly(els,[p(-m,W+m,0),p(W+m,W+m,0),p(W+m,W+m,-th),p(-m,W+m,-th)],ctx.tint(gc.L));
 ctx.poly(els,[p(W+m,W+m,0),p(L+W+m,-L+W+m,0),p(L+W+m,-L+W+m,-th),p(W+m,W+m,-th)],ctx.tint(mix(gc.L,gc.R,.5)));
 ctx.poly(els,[p(L+W+m,-L+W+m,0),p(L+W+m,-L-m,0),p(L+W+m,-L-m,-th),p(L+W+m,-L+W+m,-th)],ctx.tint(gc.R));
 ctx.poly(els,hex.map(function(h){return p(h[0],h[1],0)}),ctx.tint(gc.T));
 ctx.poly(els,[p(W+4,W+4,0),p(L+W+4,-L+W+4,0),p(L+W+8,-L+W+8,0),p(W+8,W+8,0)],ctx.tint('#E6DFD3'));
 var sel=num(props.selected,-1),tops=[];
 ts.forEach(function(t,i){ctx.seed=i+2;t.ox=i*S;t.oy=-i*S;t.crane=t.floors<t.target;t.crown='auto';t.progress=t.floors<t.target?.6:0;tops.push(drawTower(ctx,els,t));});
 ts.forEach(function(t,i){if(i<n-1){tree(ctx,els,i*S+W+8,-i*S+W-8,0,3.4);}tree(ctx,els,i*S+W/2+14,-i*S+W/2+16,0,2.6);});
 if(sel>=0&&sel<n){var tp=ctx.pt(sel*S+W/2,-sel*S+W/2,tops[sel]+(ts[sel].style==='deco'?40:32));ctx.line(els,[tp[0],tp[1]],[tp[0],tp[1]+7],AMBER,1.4);ctx.circ(els,tp,3.2,AMBER);}
 return finish(R,ctx,els,props,props.label||(n+' towers in your skyline'),num(props.minh,0));}
var api={SSTower:SSTower,SSSkyline:SSSkyline};
window.SSTower=SSTower;window.SSSkyline=SSSkyline;

})();
