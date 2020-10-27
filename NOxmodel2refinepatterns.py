# -*- coding: utf-8 -*-
"""
Created on Wed Aug 12 18:59:19 2020

@author: phari
"""

import numpy as np
import math
import matplotlib.pyplot as plt
import pandas as pd
from scipy.optimize import curve_fit
import scipy
from sklearn.metrics import mean_squared_error, mean_absolute_error, mean_poisson_deviance, mean_gamma_deviance, mean_tweedie_deviance
import random
OBD=pd.read_csv('C:/Users/panne027/UMNME452/Prof.Northrop/Prof.Northrop/100000Datatrain.csv', usecols=["IntakeT","IntakekPa","engrpm","Fuelconskgph","EGRkgph","Airinkgph","SCRinppm","EngTq", 'Wheelspeed', 'Engpwr', 'RailMPa', 'SCRingps','NOxActual', 'EINOxActual',"Anomalyindex5","Tadiab","tinj", 'NOxTheoryppmrefine2','nN2','nO2','pass'])
T1=OBD.Wheelspeed
# gt100= OBD[OBD['Wheelspeed'].gt(100)].index
# print(gt100)
# T=OBD.iloc[51937:51937+60]
# T=OBD.iloc[7492:7492+60]
# T=OBD.iloc[1035:1035+60]
T=OBD

intakeRPMlist=T['engrpm'].to_numpy()
intakePlist=T['IntakekPa'].to_numpy()*1000
intakeTlist=T['IntakeT'].to_numpy()+273
Fuelconskgph=T['Fuelconskgph'].to_numpy()
EGRkgph=T['EGRkgph'].to_numpy()
Airinkgph=T['Airinkgph'].to_numpy()
NOxActual=T['SCRinppm'].to_numpy()
Wheelspeed=T['Wheelspeed'].to_numpy()
EngTq=T['EngTq'].to_numpy()
Engpwr=T['Engpwr'].to_numpy()
RailMPa=T['RailMPa'].to_numpy()
NOxActual=T['NOxActual'].to_numpy()
SCRingps=T['SCRingps'].to_numpy()
EINOxActual=T['EINOxActual'].to_numpy()
Anomalyindex=T['Anomalyindex5'].to_numpy()
Tadiab=T['Tadiab'].to_numpy()
tinj=T['tinj'].to_numpy()
NOxTheoryppm=T['NOxTheoryppmrefine2'].to_numpy()
Nn2=T['nN2'].to_numpy()
No2=T['nO2'].to_numpy()
passnumber=T['pass'].to_numpy()

passnumber=pd.Series(passnumber)


Anomalyindex=Anomalyindex[~np.isnan(Anomalyindex)].astype(int)
#Anomalies=pd.read_csv("D:/UMN/Prof.Northrop/NOxModel2/windowpatterns/AnomalousWindows_99895_-1.0_10.0_3.csv")
#Anomalyindex=Anomalies.to_numpy()

windows=pd.read_csv("C:/Users/panne027/UMNME452/Prof.Northrop/Prof.Northrop/NOxModel2/windowpatterns/windowspatterns55254.0_3.0_0.04_41228.0_1.0_1.0.csv")

#Combustion chamber Parameters
S = 0.124#stroke (m)
B = 0.107#bore (m)
a = 0.5*S#crank radius (m) S = 2a
l = 3.5*a#connecting rod length (m)  l/a = 3-4 for small and medium size engine
Vdis = (np.pi*B**2)/4 * S;  #cylinder displacement (m^3)6 6.7L

cr = 17.3#compression ratio
Ru = 8.31434# Gas constant J/(mole K)
Vc = Vdis/(cr-1)#clearance volume (m^3)
gamma=1.35#polytropic ratio/ specific heat ratio
LHV=42.64e6# Lower Heating Value J/kg
nc=0.9; #Combustion Efficiency

IVO=-9 #Degrees ATDC

MW_fuel = 0.19065#kg/mol
MW_O2 = 0.032#kg/mol
MW_product = 0.02885#kg/mol
MW_air = 0.029#kg/mol

windowlength=3

#%%
# passindex= passnumber[passnumber.isin( [1,5,6,4,3,11,7,12])].index.tolist()
#%%

test=[]
index=[]

for k in range(0,len(NOxActual)):

    if k not in train:
        test.append(k)
    else:
        continue
#%%                          

for k in range(0,len(NOxActual)):

    if k not in test:
        train.append(k)
    else:
        continue
#%% Calculations

Vivo= (2*a*np.cos(IVO)+np.sqrt(4*a**2*np.cos(IVO)**2-4*(a**2-l**2)))/2*np.pi*B**2/4
Tpeak=intakeTlist*(Vivo/Vc)**(gamma-1)
Ppeak= (Tpeak/intakeTlist)**(gamma/(gamma-1))*intakePlist

eqratio= 14.37/(Airinkgph/(Fuelconskgph))

#intake oxygen concentration
Exhaustkgph=Airinkgph+Fuelconskgph
xexh=(0.21*Airinkgph-568/167*0.98*Fuelconskgph)/(Exhaustkgph-EGRkgph)
xo2=(0.21*Airinkgph+xexh*EGRkgph)/(Airinkgph+EGRkgph+Fuelconskgph)

#Injection duration equal to duration of combustion
fuelinjrate=0.86*7*np.pi*0.00018**2/4*np.sqrt(2*872*(RailMPa*10**6-intakePlist)) #7 holes 0.007" diameter
tinj=Fuelconskgph/(fuelinjrate*30*intakeRPMlist) #equal to residence time

#Adiabatic Flame Temperature
Tadiab=np.array([])
Nn2=np.array([])
No2=np.array([])

for i in range(len(NOxActual)):
    Energytotal=LHV*MW_fuel
    Nair= 14.37*190.649/(eqratio[i]*28.84)
    Nn2= np.append(Nn2, 0.79*Nair)
    No2=np.append(No2, (Nair-94.744)*0.21)
    a1Tadiab=(13.883*0.04453e2 + 12.026*0.02672e2 + Nn2[i]*0.02927e2 + No2[i]*0.03698e2)*Ru
    a2Tadiab2= 0.5*(13.883*0.03140e-1 + 12.026*0.03056e-1 + Nn2[i]*0.1488e-2 + No2[i]*0.06145e-2)*Ru
    coeff=[a2Tadiab2, a1Tadiab, -Energytotal]
    Tadiab=np.append(Tadiab, np.roots(coeff)[1]+Tpeak[i])

EINOxActual=SCRingps/(Fuelconskgph/3600)


#%% kvaluearray
windows= windows.to_numpy()
kindex=[]
kvaluearray=[]
# kvaluethreshold=3

for i in range(len(windows)):
    if float(math.floor(windows[i])) != windows[i]:
        kindex.append(i)
        kvaluearray.append(float(windows[i]))

#%% patterns and k values
patterns=[]
list1=[]
topk=[]
minsupp=0.004
kvaluethreshold=1
for i in range(len(kvaluearray)-1):
    if (kvaluearray[i+1]>=kvaluethreshold) and ((kindex[i+1]-kindex[i]-1)/len(windows)>=minsupp):
        list1=[]
        topk.append(kvaluearray[i+1])
        k=0
        for k in range(kindex[i]+1,kindex[i+1],1):
            list1.append(int(windows[k]))     
        patterns.append(list1)
        
    else:
        continue

#%%
topkpatterns=pd.DataFrame(data= np.column_stack((topk,patterns)), columns=['K', 'Pattern'])

topkpatterns=topkpatterns.sort_values(by='K', ascending= False)
topkpatterns.reset_index(drop=True, inplace=True)

#%% anomaly points
Anomalyindex=Anomalyindex[~np.isnan(Anomalyindex)]
NOxActualrefine=np.array([])
NOxTheoryrefine=np.array([])
anomalyindex=Anomalyindex.astype(np.int32).tolist() 
notindex=[]
for i in range(len(Anomalyindex)):
    for j in range(3):
        if Anomalyindex[i]+j not in anomalyindex:
            anomalyindex.append(int(Anomalyindex[i]+j))
            

#%% account for lag
NOxActuallead=[]
NOxActuallead[:]=NOxActual[1:]
NOxActuallead=np.append(NOxActuallead,    NOxActual [len(NOxActual)-1])

EINOxActuallead=[]
EINOxActuallead[:]=EINOxActual[1:]
EINOxActuallead=np.append(EINOxActuallead, EINOxActual[len(EINOxActual)-1])

NOxTheoryppmbase=NOxTheoryppm
#%% Divergent patterns dataset splitting *******
npatterns=4
index1=[]
patternpoints= [[] for _ in range(npatterns)]
tinjpatterns=[[]for _ in range(npatterns)]
Tadiabpatterns=[[]for _ in range(npatterns)]
EINOxActualpatterns=[[]for _ in range(npatterns)]
# NOxTheoryupdate=np.array([])

pattern1=[]

for i in range(npatterns):
    pattern1=[]
    pattern1=topkpatterns.Pattern[i]
    for j in range(len(pattern1)):
        if pattern1[j] in Anomalyindex:
            
            for k in range(windowlength):
                if (pattern1[j]+k not in topkpatterns.Pattern[i]) and (pattern1[j]+k<pattern1[-1]) :
                    patternpoints[i].append(int(pattern1[j]+k))
                    EINOxActualpatterns[i].append(EINOxActuallead[int(pattern1[j]+k)])
                    Tadiabpatterns[i].append(Tadiab[int(pattern1[j]+k)])
                    tinjpatterns[i].append(tinj[int(pattern1[j]+k)])
                    
                else:
                        continue


notindex=[]
index=[]

for k in range(0,len(NOxActual)):

    if not any(k in pattern for pattern in patternpoints):
        notindex.append(k)
    else:
        index.append(k)
        
        #%%
random2=random.sample(range(len(notindex)),30000)
train225=[]
train225=np.array(notindex)
train225=train225[random2]

        
#%%



#%% notindex plot
plt.rcParams.update({'font.size': 45})
plt.figure(figsize=(35,35))

plt.plot(range(0,int(1000), 1),range(0,int(1000), 1))
plt.xlim(0, 1000)
plt.ylim(0, 1000)
plt.title('Divergent anomaly index rpm minsupp=0.01 kvaluethres=1 ',fontsize=45)
plt.xlabel('NOx Observed/ppm)', fontsize=45)
plt.ylabel('NOx Prediction /ppm', fontsize=45)   
plt.scatter(NOxActual[index], NOxTheoryppmbase[index])


#%%
plt.rcParams.update({'font.size': 45})
NOxActualold=[]
NOxTheoryppmold=[]

# plt.plot(range(0,int(1000), 1),range(0,int(1000), 1))
# plt.xlim(0, 1500)
# plt.ylim(0, 1500)
# plt.title('Mined co-occurrence pattterns minsupp=0.01 kvaluethres=1 ',fontsize=45)
# plt.xlabel('NOx Observed/ppm)', fontsize=45)
# plt.ylabel('NOx Prediction /ppm', fontsize=45)

fig1,ax1=plt.subplots(2,2, sharex=False, sharey=False, figsize=(35,35),constrained_layout=True)

# ax1[0,0].xlim(0, 1500)
# ax1[0,0].ylim(0, 1500)
sc=ax1[0,0].scatter(NOxActual[patternpoints[0]], NOxTheorypatterns[0],c=abs(NOxTheorypatterns[0]-NOxActual[patternpoints[0]]), cmap='viridis')
ax1[0,0].set_title('DWC Pattern1 ',fontsize=45)
ax1[0,0].set_xlabel('NOx Observed/ppm)', fontsize=45)
ax1[0,0].set_ylabel('NOx Prediction /ppm', fontsize=45)
ax1[0,0].plot(range(0,int(1500), 1),range(0,int(1500), 1))
sc.set_clim(vmin=0, vmax=1000)

ax1[0,0].set_xlim(0, 1500)
ax1[0,0].set_ylim(0, 1500)
sc=ax1[0,1].scatter(NOxActual[patternpoints[1]], NOxTheorypatterns[1],c=abs(NOxTheorypatterns[1]-NOxActual[patternpoints[1]]), cmap='viridis')
ax1[0,1].set_title('DWC Pattern2 ',fontsize=45)
ax1[0,1].set_xlabel('NOx Observed/ppm)', fontsize=45)
ax1[0,1].set_ylabel('NOx Prediction /ppm', fontsize=45)
ax1[0,1].plot(range(0,int(1500), 1),range(0,int(1500), 1))

ax1[0,1].set_xlim(0, 1500)
ax1[0,1].set_ylim(0, 1500)
sc.set_clim(vmin=0, vmax=1000)

sc=ax1[1,0].scatter(NOxActual[patternpoints[2]], NOxTheorypatterns[2],c=abs(NOxTheorypatterns[2]-NOxActual[patternpoints[2]]), cmap='viridis')
ax1[1,0].set_title('DWC Pattern3 ',fontsize=45)
ax1[1,0].set_xlabel('NOx Observed/ppm)', fontsize=45)
ax1[1,0].set_ylabel('NOx Prediction /ppm', fontsize=45)
ax1[1,0].plot(range(0,int(1500), 1),range(0,int(1500), 1))

ax1[1,0].set_xlim(0, 1500)
ax1[1,0].set_ylim(0, 1500)
sc.set_clim(vmin=0, vmax=1000)

sc=ax1[1,1].scatter(NOxActual[patternpoints[3]], NOxTheorypatterns[3],c=abs(NOxTheorypatterns[3]-NOxActual[patternpoints[3]]), cmap='viridis')
ax1[1,1].set_title('DWC Pattern4 ',fontsize=45)
ax1[1,1].set_xlabel('NOx Observed/ppm)', fontsize=45)
ax1[1,1].set_ylabel('NOx Prediction /ppm', fontsize=45)
ax1[1,1].plot(range(0,int(1500), 1),range(0,int(1500), 1))
ax1[1,1].set_xlim(0, 1500)
ax1[1,1].set_ylim(0, 1500) 
sc.set_clim(vmin=0, vmax=1000)
cbar = fig1.colorbar(sc, ax=ax1.ravel().tolist(), shrink=0.95)

plt.show()
   # NOxActualold=np.append(NOxActualold,NOxActual[patternpoints[i]])
    # NOxTheoryppmold=np.append(NOxTheoryppmold, NOxTheoryppm[patternpoints[i]])
    
# r_value= scipy.stats.linregress(NOxActualold, NOxTheoryppmold)
# R2value=r_value[2]**2
# print("R2value:",R2value)
# PValue=r_value[3]

# rmse=np.sqrt(mean_squared_error(NOxActualold, NOxTheoryppmold))
# print("RMSE:",rmse)
# mae=mean_absolute_error(NOxActualold, NOxTheoryppmold)
# print("MAE",mae)

# pattern1[j]+k not in patternpoints[i]

#%% Divergent patterns dataset splitting and refinement
fitParams=[]
index1=[]
patternpoints= [[] for _ in range(len(patterns))]
tinjpatterns=[[]for _ in range(len(patterns))]
Tadiabpatterns=[[]for _ in range(len(patterns))]
EINOxActualpatterns=[[]for _ in range(len(patterns))]
NOxTheoryupdate=np.array([])

def EINOxpredict(X, a, b, c):
    Tadiab, tinj =X
    return a*(Tadiab**b)*(tinj**c)

pattern1=[]

for i in range(np.size(patterns)):
    pattern1=[]
    pattern1=patterns[i]
    p0=25e5, -1.5, 0.5
    for j in range(len(pattern1)):
        
        for k in range(windowlength):
            if pattern1[j]+k not in patternpoints[i]:
                patternpoints[i].append(int(pattern1[j]+k))
                EINOxActualpatterns[i].append(EINOxActuallead[int(pattern1[j]+k)])
                Tadiabpatterns[i].append(Tadiab[int(pattern1[j]+k)])
                tinjpatterns[i].append(tinj[int(pattern1[j]+k)])
                
            else:
                continue
    fitParams[i], fitCovar= curve_fit(EINOxpredict, (Tadiabpatterns[i][:] , tinjpatterns[i][:]), EINOxActualpatterns[i][:], p0, maxfev=1000000, method='lm')
    EINOxTheorypatterns[i]= EINOxpredict((Tadiab[pattern1],tinj[pattern1]), fitParams[i][0], fitParams[i][1], fitParams[i][2])
    molesNOx= EINOxTheorypatterns[i]/38 * Fuelconskgph[patternpoints[i]] #moles per hour
    totalproductmoles= (Nn2[patternpoints[i]] + No2[patternpoints[i]] + 13.883 + 12.026)* Fuelconskgph[patternpoints[i]]/MW_fuel +molesNOx #moles per hour
    NOxTheorypatterns[i]= molesNOx/totalproductmoles*1e6
    NOxTheoryppm[pattern1]=NOxTheorypatterns[i]
   
 # pattern1[j]+k not in patternpoints[i]
#%%       

# NOxActualpatterns[i]=np.append(NOxActualpatterns[i],NOxActuallead[int(pattern1[j]+k)])
#                 Tadiabpatterns[i]=np.append(Tadiabpatterns[i],Tadiab[int(pattern1[j]+k)])
#                 tinjpatterns[i]=np.append(tinjpatterns[i],tinj[int(pattern1[j]+k)])
#                 #NOxTheoryrefine=np.append(NOxTheoryrefine,NOxTheoryppm[int(Anomalyindex[i])+j])
        

#%%Curve Fit baseline
NOxTheoryppmnew=NOxTheoryppm

def EINOxpredict(X, a, b, c):
    Tadiab, tinj =X
    return a*(Tadiab**b)*(tinj**c)

X=['Tadiab', 'tinj']

p0=25e5, -1.5, 0.5

fitParams, fitCovar= curve_fit(EINOxpredict, (Tadiab[train], tinj[train]), EINOxActuallead[train], p0, maxfev=1000000)
print(fitParams)
print(fitCovar)

EINOxTheory= EINOxpredict((Tadiab[:],tinj[:]), fitParams[0], fitParams[1], fitParams[2])

molesNOx= EINOxTheory/38 * Fuelconskgph[:] #moles per hour
totalproductmoles= (Nn2[:] + No2[:] + 13.883 + 12.026)* Fuelconskgph[:]/MW_fuel +molesNOx #moles per hour
NOxTheoryppmnotindex= molesNOx/totalproductmoles*1e6
NOxActualold=[]
NOxTheoryold=[]
NOxTheoryppmnew[test]=NOxTheoryppmnotindex[notindex]
plt.rcParams.update({'font.size': 45})
plt.figure(figsize=(35,35))

plt.plot(range(0,int(1000), 1),range(0,int(1000), 1))
plt.xlim(0, 1000)
plt.ylim(0, 1000)
plt.title('Baseline model considering pattterns',fontsize=45)
plt.xlabel('NOx Observed/ppm)', fontsize=45)
plt.ylabel('NOx Prediction /ppm', fontsize=45)
plt.scatter(NOxActual[test], NOxTheoryppmnew[test] )
r_value= scipy.stats.linregress(NOxActual, NOxTheoryppmnew[test])
R2value=r_value[2]**2
print("R2value:",R2value)
PValue=r_value[3]

rmse=np.sqrt(mean_squared_error(NOxActual, NOxTheoryppmnew[test]))
print("RMSE:",rmse)
mae=mean_absolute_error(NOxActual, NOxTheoryppmnew[test])
print("MAE",mae)


#%% separate plots new refined
plt.rcParams.update({'font.size': 45})
plt.figure(figsize=(35,35))

plt.plot(range(0,int(1000), 1),range(0,int(1000), 1))
plt.xlim(0, 1000)
plt.ylim(0, 1000)
plt.title('Different Patterns detected',fontsize=45)
plt.xlabel('NOx Observed/ppm)', fontsize=45)
plt.ylabel('NOx Prediction /ppm', fontsize=45)
for i in range(len(patterns)):
   
    plt.scatter(NOxActual[patternpoints[i]], NOxTheoryppmnew[patternpoints[i]] )

# r_value= scipy.stats.linregress(NOxActual, NOxTheoryppm)
# R2value=r_value[2]**2
# print("R2value:",R2value)
# PValue=r_value[3]

# rmse=np.sqrt(mean_squared_error(NOxActual, NOxTheoryppmold))
# print("RMSE:",rmse)
# mae=mean_absolute_error(NOxActual, NOxTheoryppmold)
# print("MAE",mae)


#%% Refined prediction
def EINOxpredict(X, a, b, c):
    Tadiab, tinj =X
    return a*(Tadiab**b)*(tinj**c)

fitParams=[[] for _ in range(len(patterns))]
EINOxTheorypatterns =[[] for _ in range(len(patterns))]
NOxTheorypatterns =[[] for _ in range(len(patterns))]

NOxActualnew=[]
NOxTheorynew=[]

plt.rcParams.update({'font.size': 45})
plt.figure(figsize=(35,35))
plt.plot(range(0,int(1000), 1),range(0,int(1000), 1))
plt.xlim(0, 1000)
plt.ylim(0, 1000)
plt.title('Refined models using pattterns',fontsize=45)
plt.xlabel('NOx Observed/ppm)', fontsize=45)
plt.ylabel('NOx refined Prediction /ppm', fontsize=45)

for i in range(len(patterns)):
    p0=25e10, -1.5, 0.5
    fitParams[i], fitCovar= curve_fit(EINOxpredict, (Tadiabpatterns[i][:] , tinjpatterns[i][:]), EINOxActualpatterns[i][:], p0, maxfev=1000000, method='lm')
    EINOxTheorypatterns[i]= EINOxpredict((Tadiabpatterns[i][:],tinjpatterns[i][:]), fitParams[i][0], fitParams[i][1], fitParams[i][2])
    molesNOx= EINOxTheorypatterns[i]/38 * Fuelconskgph[patternpoints[i]] #moles per hour
    totalproductmoles= (Nn2[patternpoints[i]] + No2[patternpoints[i]] + 13.883 + 12.026)* Fuelconskgph[patternpoints[i]]/MW_fuel +molesNOx #moles per hour
    NOxTheorypatterns[i]= molesNOx/totalproductmoles*1e6
    plt.scatter(NOxActual[patternpoints[i]], NOxTheorypatterns[i])
    NOxActualnew = np.append(NOxActualnew,NOxActual[patternpoints[i]])
    NOxTheorynew = np.append(NOxTheorynew,NOxTheorypatterns[i])

# cbar=plt.colorbar()
# cbar.set_label('Divergence /ppm', fontsize=45)
r_value= scipy.stats.linregress(NOxActualnew, NOxTheorynew)
R2value=r_value[2]**2
print("R2value:",R2value)
PValue=r_value[3]

rmse=np.sqrt(mean_squared_error(NOxActualnew, NOxTheorynew))
print("RMSE:",rmse)
mae=mean_absolute_error(NOxActualnew, NOxTheorynew)
print("MAE",mae)



#%% Refined prediction 2 ****
def EINOxpredict(X, a, b, c):
    Tadiab, tinj =X
    return a*(Tadiab**b)*(tinj**c)

fitParams=[[] for _ in range(npatterns)]
EINOxTheorypatterns =[[] for _ in range(npatterns)]
NOxTheorypatterns =[[] for _ in range(npatterns)]

# NOxActualnew=[]
NOxTheoryppmnew=np.array(NOxTheoryppmbase)

plt.rcParams.update({'font.size': 45})
plt.figure(figsize=(35,35)) 


for i in range(npatterns):
    p0=5e10, -1.5, 2
    fitParams[i], fitCovar= curve_fit(EINOxpredict, (Tadiabpatterns[i][:] , tinjpatterns[i][:]), EINOxActualpatterns[i][:], p0, maxfev=1000000, method='dogbox')
    EINOxTheorypatterns[i]= EINOxpredict((Tadiabpatterns[i][:],tinjpatterns[i][:]), fitParams[i][0], fitParams[i][1], fitParams[i][2])
    molesNOx= EINOxTheorypatterns[i]/38 * Fuelconskgph[patternpoints[i]] #moles per hour
    totalproductmoles= (Nn2[patternpoints[i]] + No2[patternpoints[i]] + 13.883 + 12.026)* Fuelconskgph[patternpoints[i]]/MW_fuel +molesNOx #moles per hour
    NOxTheorypatterns[i]= molesNOx/totalproductmoles*1e6
    
    for j in range(len(patternpoints[i])):
        if NOxTheoryppmnew[patternpoints[i][j]]>NOxTheorypatterns[i][j]:
            NOxTheoryppmnew[int(patternpoints[i][j])] = NOxTheorypatterns[i][j]
        else:
            continue


plt.scatter(NOxActual[:], NOxTheoryppmnew[:], c=abs(NOxTheoryppmnew[:]-NOxActual[:]), cmap='viridis' )
plt.plot(range(0,int(1500), 1),range(0,int(1500), 1))
# plt.xlim(0, 1000)
# plt.ylim(0, 1000)
plt.title('Refined models using pattterns',fontsize=45)
plt.xlabel('NOx Observed/ppm)', fontsize=45)
plt.ylabel('NOx refined Prediction /ppm', fontsize=45)
cbar=plt.colorbar()
cbar.set_label('Divergence /ppm', fontsize=45)

r_value= scipy.stats.linregress(NOxActual[:], NOxTheoryppmnew[:])
R2value=r_value[2]**2
print("R2value:",R2value)
PValue=r_value[3]

rmse=np.sqrt(mean_squared_error(NOxActual[:], NOxTheoryppmnew[:]))
print("RMSE:",rmse)
mae=mean_absolute_error(NOxActual[:], NOxTheoryppmnew[:])
print("MAE",mae)

#%%Second curvefit train2 ******
def EINOxpredict(X, a, b, c):
    Tadiab, tinj =X
    return a*(Tadiab**b)*(tinj**c)

p0=5e7, -1.5, 0.5
fitParams2, fitCovar= curve_fit(EINOxpredict, (Tadiab[notindex] , tinj[notindex]), EINOxActual[notindex], p0, maxfev=1000000, method='lm')
EINOxTheory2= EINOxpredict((Tadiab[:],tinj[:]),  fitParams2[0], fitParams2[1], fitParams2[2])
molesNOx= EINOxTheory2[:]/38 * Fuelconskgph[:] #moles per hour
totalproductmoles= (Nn2[:] + No2[:] + 13.883 + 12.026)* Fuelconskgph[:]/MW_fuel +molesNOx #moles per hour
NOxTheoryppmrefine2= molesNOx/totalproductmoles*1e6
r_value= scipy.stats.linregress(NOxActual[:], NOxTheoryppmrefine2[:])
R2value=r_value[2]**2
print(fitParams2)
print(fitCovar)

print("R2value:",R2value)
PValue=r_value[3]

rmse=np.sqrt(mean_squared_error(NOxActual[:], NOxTheoryppmrefine2[:]))
print("RMSE:",rmse)
mae=mean_absolute_error(NOxActual[:], NOxTheoryppmrefine2[:])
print("MAE",mae)  

#%%Second curvefit testing and training data 
def EINOxpredict(X, a, b, c):
    Tadiab, tinj =X
    return a*(Tadiab**b)*(tinj**c)

p0=5e7, -1.5, 0.5
fitParams2, fitCovar= curve_fit(EINOxpredict, (Tadiab[train] , tinj[train]), EINOxActual[train], p0, maxfev=1000000, method='lm')
EINOxTheory2= EINOxpredict((Tadiab[:],tinj[:]),  fitParams2[0], fitParams2[1], fitParams2[2])
molesNOx= EINOxTheory2[:]/38 * Fuelconskgph[:] #moles per hour
totalproductmoles= (Nn2[:] + No2[:] + 13.883 + 12.026)* Fuelconskgph[:]/MW_fuel +molesNOx #moles per hour
NOxTheoryppmrefine2= molesNOx/totalproductmoles*1e6
r_value= scipy.stats.linregress(NOxActual[:], NOxTheoryppmrefine2[:])
R2value=r_value[2]**2
print(fitParams2)
print(fitCovar)

print("R2value:",R2value)
PValue=r_value[3]

rmse=np.sqrt(mean_squared_error(NOxActual[:], NOxTheoryppmrefine2[:]))
print("RMSE:",rmse)
mae=mean_absolute_error(NOxActual[:], NOxTheoryppmrefine2[:])
print("MAE",mae)  
#%%
plt.rcParams.update({'font.size': 45})
plt.figure(figsize=(35,35))
plt.scatter(NOxActual[:], NOxTheoryppmrefine2[:], c=abs(NOxTheoryppmrefine2[:]-NOxActual[:]), cmap='viridis' )
plt.plot(range(0,int(1000), 1),range(0,int(1000), 1))
plt.xlim(0, 1500)
plt.ylim(0, 1500)
plt.title('Refined prediction using R-DWC method',fontsize=45)
plt.xlabel('NOx Observed/ppm)', fontsize=45)
plt.ylabel('NOx Predicted /ppm', fontsize=45)
cbar=plt.colorbar()

cbar.set_label('Divergence /ppm', fontsize=45)


#%%Second curvefit plus baseline test ********


# fitParamsbase, fitCovar= curve_fit(EINOxpredict, (Tadiab[:] , tinj[:]), EINOxActual[:], p0, maxfev=1000000, method='lm')
# EINOxTheory2= EINOxpredict((Tadiab[:],tinj[:]),  fitParamsbase[0], fitParamsbase[1], fitParamsbase[2])
# molesNOx= EINOxTheory2[:]/38 * Fuelconskgph[:] #moles per hour
# totalproductmoles= (Nn2[:] + No2[:] + 13.883 + 12.026)* Fuelconskgph[:]/MW_fuel +molesNOx #moles per hour
# NOxTheoryppmrefine2= molesNOx/totalproductmoles*1e6
NOxTheoryppmrefine3=NOxTheoryppmrefine2

for i in range(npatterns):
    for j in range(len(patternpoints[i])):
        if NOxTheoryppmrefine3[patternpoints[i][j]]>NOxTheorypatterns[i][j]:
            NOxTheoryppmrefine3[int(patternpoints[i][j])] = NOxTheorypatterns[i][j]
        else:
            continue

r_value= scipy.stats.linregress(NOxActual[:], NOxTheoryppmrefine3[:])
R2value=r_value[2]**2
# print("R2value:",R2value)
PValue=r_value[3]

rmse=np.sqrt(mean_squared_error(NOxActual[:], NOxTheoryppmrefine3[:]))
# print("RMSE:",rmse)
mae=mean_absolute_error(NOxActual[:], NOxTheoryppmrefine3[:])
print(R2value,rmse,mae)  
#%%
plt.rcParams.update({'font.size': 45})
plt.figure(figsize=(35,35))
plt.scatter(NOxActual[:], NOxTheoryppmrefine3[:], c=abs(NOxTheoryppmrefine3[:]-NOxActual[:]), cmap='viridis' )
plt.plot(range(0,int(1500), 1),range(0,int(1500), 1))
plt.xlim(0, 1500)
plt.ylim(0, 1500)
plt.title('Refined Prediction using DWC patterns ',fontsize=45)
plt.xlabel('NOx Observed/ppm)', fontsize=45)
plt.ylabel('NOx Predicted /ppm', fontsize=45)
cbar=plt.colorbar()
plt.clim(0,1000)
cbar.set_label('Divergence /ppm', fontsize=45)

#%%Second curvefit plus baseline train *******
# fitParamsbase, fitCovar= curve_fit(EINOxpredict, (Tadiab[:] , tinj[:]), EINOxActual[:], p0, maxfev=1000000, method='lm')
# EINOxTheory2= EINOxpredict((Tadiab[:],tinj[:]),  fitParamsbase[0], fitParamsbase[1], fitParamsbase[2])
# molesNOx= EINOxTheory2[:]/38 * Fuelconskgph[:] #moles per hour
# totalproductmoles= (Nn2[:] + No2[:] + 13.883 + 12.026)* Fuelconskgph[:]/MW_fuel +molesNOx #moles per hour
# NOxTheoryppmrefine2= molesNOx/totalproductmoles*1e6
NOxTheoryppmrefine3=NOxTheoryppmrefine2

for i in range(npatterns):
    for j in range(len(patternpoints[i])):
        if NOxTheoryppmrefine3[patternpoints[i][j]]>NOxTheorypatterns[i][j]:
            NOxTheoryppmrefine3[int(patternpoints[i][j])] = NOxTheorypatterns[i][j]
        else:
            continue

r_value= scipy.stats.linregress(NOxActual[train], NOxTheoryppmrefine3[train])
R2value=r_value[2]**2
print("R2value:",R2value)
PValue=r_value[3]

rmse=np.sqrt(mean_squared_error(NOxActual[train], NOxTheoryppmrefine3[train]))
print("RMSE:",rmse)
mae=mean_absolute_error(NOxActual[train], NOxTheoryppmrefine3[train])
print("MAE",mae)  

#%% Baseline plot
plt.rcParams.update({'font.size': 45})
plt.figure(figsize=(35,35))

plt.plot(range(0,int(1500), 1),range(0,int(1500), 1))
plt.xlim(0, 1500)
plt.ylim(0, 1500)
plt.title('Baseline Physics-Aware Data-Driven Model Prediction',fontsize=45)
plt.xlabel('NOx Observed/ppm)', fontsize=45)
plt.ylabel('NOx Prediction /ppm', fontsize=45)
plt.scatter(NOxActual[:], NOxTheoryppm[:], c=abs(NOxTheoryppm[:]-NOxActual[:]), cmap='viridis' )
cbar=plt.colorbar()
plt.clim(0,1000)
cbar.set_label('Divergence /ppm', fontsize=45)
r_value= scipy.stats.linregress(NOxActual, NOxTheoryppm)
R2value=r_value[2]**2
print("R2value:",R2value)
PValue=r_value[3]

rmse=np.sqrt(mean_squared_error(NOxActual, NOxTheoryppm))
print("RMSE:",rmse)
mae=mean_absolute_error(NOxActual, NOxTheoryppm)
print("MAE",mae)


#%%
r_value= scipy.stats.linregress(NOxActual[:], NOxTheoryppm[:])
R2value=r_value[2]**2

print("R2value:",R2value)
PValue=r_value[3]

rmse=np.sqrt(mean_squared_error(NOxActual[:], NOxTheoryppm[:]))
print("RMSE:",rmse)
mae=mean_absolute_error(NOxActual[:], NOxTheoryppm[:])
print("MAE",mae)
