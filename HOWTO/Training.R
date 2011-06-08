# SETTINGS
propertyfile <- "./MassStruct.properties"

# reading properties from config file (MassStruct.properties)
properties <- read.table(	file=file, sep="=",
				row.names = 1,
				comment.char = "#",
				stringsAsFactors=FALSE)
# VARIABLES
# PostgreSQL related variables
DB_HOST <- properties["HOST",]
DB_NAME <- properties["DATABASE",]

# database related variables
DB_DRV <- properties["DATABASE_DRIVER",]
USER <- properties["DB_USER",]
PASSWD <- properties["DB_PASSWORD",]

# table related variables
FRAGMENT_TABLE <- properties["TAB_FRAGMENTS",]
EXACT_MASS_COLOUMN <- properties["COL_EXACT_MASS",]
STRUCTURE_COLOUMN <- properties["COL_STRUCTURE",]

MZ_CLUSTER_TABLE <- properties["TAB_MZ_CLUSTER",]
MIN_MZ_COLOUMN <- properties["COL_MIN_MZ",]
MAX_MZ_COLOUMN <- properties["COL_MAX_MZ",]

MZ_CLUSTER_STRUCT_TABLE <- properties["TAB_MZ_CLUSTER_STRUCT",]
MIN_MZ_COLOUMN <- properties["COL_MIN_MZ_COLOUMN",]
MAX_MZ_COLOUMN <- properties["MAX_MZ_COLOUMN",]

# clustering related variables
MZ_CUTTREE <- as.numeric(properties["MZ_CUT",])
TANIMOTO_CUTTREE <- as.numeric(properties["TM_CUT",])


# loading modules
library(rcdk)
library(RPostgreSQL)



# opening connection to host/database
drv <- dbDriver(DB_DRV)
con <- dbConnect(drv, host=DB_HOST, dbname=DB_NAME, uid=USER, pwd=PASSWD)

# getting m_z values and fragments from DB
mzs <- dbGetQuery(con, pase("SELECT ",EXACT_MASS_COLOUMN,", ",STRUCTURE_COLOUMN," FROM ",FRAGMENT_TABLE,";"))

# biuld distance matrix of m_z values
d <- dist(mzs[,1])

# hier. clustering of m-z values
h <- hclust(d)

# cut the hier. tree at h
classlabel <- as.factor(cutree(h, h=MZ_CUTTREE))

# convert m_z cluster range into matrix with min and max values
mz_clusterreps <- matrix(data=unlist(tapply(mzs[,1], classlabel, range)), ncol=2, byrow=T)

#---mz-Cluster in Tabelle speichern---
for(i in 1:length(mz_clusterreps[,1])){                                          
	dbSendQuery(con, paste("INSERT INTO",MZ_CLUSTER_TABLE," (",MIN_MZ_COLOUMN,MAX_MZ_COLOUMN,") VALUES (",mz_clusterreps[i,1],", ",mz_clusterreps[i,2],");"))
}
##############################################################################
#                  INSERT INTO MZ_CLUSTERSTRUCT                              #
##############################################################################
#---Strukturen in Hilfstabelle speichern---3049 jeweils zu einer mz_range_id von oben
#####ÜBERARBEITEN!!!!!!

for(i in 1:length(mzs[,2])){
	dbSendQuery(con, paste("INSERT INTO ", MZ_CLUSTER_STRUCT_TABLE, " ("cluster_id, substructure) values ((SELECT cluster_id FROM mz_cluster WHERE (",mzs[i,1],"BETWEEN mz_cluster.min AND mz_cluster.max)), '",mzs[i,2],"');",sep=""))
}
#####ÜBERARBEITEN ENDE!!!!

# defining mcs function for using in each vector of substructures in one mz-cluster
mcss <- function(x) {
  l <- length(x)
  
  if (l==1)
    return (x[[1]])
  
  consensus <- x[[1]]  
  for (i in 2:l) {
    consensus <- get.mcs(consensus, x[[i]])
  }
  return (consensus)  
}

# getting structures of each mz_cluster from DB and calculate tanimoto cluster and mcs's of each tanimoto cluster and write back to DB
for(i in 1:length(mz_clusterreps[,1])){
	mz_cluster <- dbGetQuery(con, paste("SELECT substructure FROM mz_cluster_struct WHERE (cluster_id = ",i,");",sep=""))
	if(length(mz_cluster[,1]) > 1){	
		mz_cluster <- sapply(mz_cluster, parse.smiles)
		fps <- lapply(mz_cluster, get.fingerprint, type = "extended")
		fp.sim <- fp.sim.matrix(fps, method = "tanimoto")
		fp.dist <- 1 - fp.sim
		tancluster <- hclust(as.dist(fp.dist))
##############################################################################
#                          TANIMOTO CUTTREE                                  #
##############################################################################
		classlabel <- as.factor(cutree(tancluster, h=1.0))
		mcstanclusterreps <- tapply(mz_cluster, classlabel, mcss)
##############################################################################
#                          INSERT INTO MCS                                   #
##############################################################################
		for(k in 1:length(mcstanclusterreps)){
			dbSendQuery(con, paste("INSERT INTO mcs (mcs_structure, mz_cluster_id) VALUES ('",get.smiles(mcstanclusterreps[[k]]),"', ",i,");",sep=""))
		}
	} else {
		dbSendQuery(con, paste("INSERT INTO mcs (mcs_structure, mz_cluster_id) VALUES ('",mz_cluster[[1]],"', ",i,");",sep=""))
	}	
}


